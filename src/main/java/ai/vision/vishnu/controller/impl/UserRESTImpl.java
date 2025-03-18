package ai.vision.vishnu.controller.impl;

import ai.vision.vishnu.controller.UserREST;
import ai.vision.vishnu.model.UserBean;
import ai.vision.vishnu.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
public class UserRESTImpl implements UserREST {
    @Autowired
    private UserService userService;

    @GetMapping("/user/{id}")
    @Override
    public ResponseEntity<UserBean> getUser(@PathVariable Long uid) {
        return ResponseEntity.ok(UserBean.from(userService.getUser(uid)));
    }

    @GetMapping("/user/{email}")
    @Override
    public ResponseEntity<UserBean> getUser(@PathVariable String email) {
        return ResponseEntity.ok(UserBean.from(userService.getUser(email)));
    }

//
//    @GetMapping("/")
//    public String home(Model model, @AuthenticationPrincipal OidcUser principal) {
//        if (principal != null) {
//            model.addAttribute("profile", principal.getClaims());
//        }
//        return "index";
//    }


}