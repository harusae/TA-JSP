package kr.co.wisenut.controller.user;

import kr.co.wisenut.entity.Lv1Info;
import kr.co.wisenut.entity.RealtimeKeywordChartInfo;
import kr.co.wisenut.entity.UserInfo;
import kr.co.wisenut.service.RealtimeKeywordService;
import kr.co.wisenut.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserRestController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UserService userService;
    @Autowired
    private RealtimeKeywordService realtimeKeywordService;

    @RequestMapping(method= RequestMethod.POST, value="/myPage/info")
    @ResponseBody
    public ResponseEntity getMyPageInfo(@RequestParam HashMap<String, Object> param, Authentication auth){
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        param.put("userId", userDetails.getUsername());
        logger.info("getMyPageInfo param : {}", param);

        UserInfo userInfo = userService.getMyPageInfo(param);

        return new ResponseEntity(userInfo, HttpStatus.OK);
    }

    @RequestMapping(method= RequestMethod.POST, value="/myPage/changeInfo")
    @ResponseBody
    public ResponseEntity changeInfo(@RequestParam HashMap<String, Object> param, Authentication auth, HttpServletRequest request){
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        param.put("userId", userDetails.getUsername());
        logger.info("changeInfo param : {}", param);

        String errorMsg = "";
        int result = userService.changeInfo(param);
        logger.info("changeInfo result : {}", result);
        logger.info("result param : {}", param);
        switch(result) {
            case 0:
                errorMsg = "패스워드 변경실패";
                break;
            case -1:
                errorMsg = "패스워드가 틀립니다.";
                break;
            case -2:
                errorMsg = "최근 3회 변경한 패스워드와 동일합니다.";
                break;
            case -3:
                errorMsg = "내정보 변경실패";
                break;
        }

        //이력저장 예외 > 미암호화 비밀번호 제거 후 이력저장
        HashMap<String, Object> historyParam = new HashMap<>();
        historyParam.put("actionType", "INQUERY");
        historyParam.put("resourceId", "2001001");
        historyParam.put("resourceType", "USER_ACTION");
        historyParam.put("actionMsg", "비밀번호 변경");
        historyParam.put("actionMsg", request.getRequestURI());
        historyParam.put("actionUser", userDetails.getUsername());
        historyParam.put("params", historyParam.toString());
        historyParam.put("userIp", request.getRemoteAddr());

        //비밀번호 변경이 포함된 경우
        if(param.containsKey("userPw")){
            historyParam.put("actionType", "LOGIN");
            historyParam.put("resourceId", "1001003");  //패스워드 변경 성공
            historyParam.put("resourceType", "PASSWORD_CHANGE");
            historyParam.put("actionMsg", "비밀번호 변경");
            historyParam.put("actionUser", param.get("userId"));
            historyParam.put("params", param.toString());
            historyParam.put("userIp", request.getRemoteAddr());

            if(result < 1){ //패스워드 변경 실패 시
                historyParam.put("resourceId", "1001004");  //패스워드 변경 실패
                historyParam.put("resourceType", "PASSWORD_CHANGE_FAIL");
                historyParam.put("actionMsg", errorMsg);
            }
        }

        userService.insertActionHistory(historyParam);

        //변경 실패 시 실패 리턴
        if(result < 1) {
            return new ResponseEntity(errorMsg, HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity(result, HttpStatus.OK);
    }


    @RequestMapping(method= RequestMethod.POST, value="/realtimeKeyword/lv1List")
    @ResponseBody
    public ResponseEntity getLv1List(@RequestParam HashMap<String, Object> param, Authentication auth){
        logger.info("getLv1List param : {}", param);

        List<Lv1Info> list = realtimeKeywordService.getLv1ClsList(param);

        return new ResponseEntity(list, HttpStatus.OK);
    }

    @RequestMapping(method= RequestMethod.POST, value="/realtimeKeyword/getData")
    @ResponseBody
    public ResponseEntity getRealtimeKeywordData(@RequestParam HashMap<String, Object> param, Authentication auth){
        logger.info("getRealtimeKeywordData param : {}", param);

        List<List<List<Object>>> list = realtimeKeywordService.getRealtimeKeywordList(param);

        return new ResponseEntity(list, HttpStatus.OK);
    }

    @RequestMapping(method= RequestMethod.POST, value="/realtimeKeyword/lv1ChartList")
    @ResponseBody
    public ResponseEntity getLv1ChartList(@RequestParam HashMap<String, Object> param, Authentication auth){
        logger.info("getLv1ChartList param : {}", param);

        List<RealtimeKeywordChartInfo> list = realtimeKeywordService.getLv1ChartList(param);

        return new ResponseEntity(list, HttpStatus.OK);
    }

}