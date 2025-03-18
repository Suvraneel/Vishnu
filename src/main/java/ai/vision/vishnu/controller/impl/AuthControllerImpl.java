package ai.vision.vishnu.controller.impl;

import ai.vision.vishnu.controller.AuthController;
import ai.vision.vishnu.entity.User;
import ai.vision.vishnu.model.UserBean;
import ai.vision.vishnu.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthControllerImpl implements AuthController {
    @Autowired
    private UserService userService;
//    @GetMapping("/me")
//    public UserBean whoAmI(@AuthenticationPrincipal OidcUser oidcUser) {
//        log.info("Getting user info");
//        UserBean userBean = new UserBean();
//        userBean.setEmail(oidcUser.getEmail());
//        userBean.setName(oidcUser.getFullName());
//        userBean.setPicture(oidcUser.getPicture());
//        return userBean;
//    }

    @PostMapping("/register")
    @Override
    public ResponseEntity<String> register(@RequestBody UserBean userBean) throws BadRequestException {
        log.info("Registering user: {}", userBean);
        User user = userService.createUser(userBean);
        if (Objects.nonNull(user)) {
            log.info("User {} registered successfully!", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(String.format("User %s registered successfully!", user.getUsername()));
        }
        log.error("User registration failed: {}", userBean);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User registration failed");
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<UserBean> login(@RequestBody UserBean userBean, HttpSession session) {
        log.info("Attempting to login user: {}", userBean);
        boolean isAuthenticated = userService.authenticateUser(userBean.getEmail(), userBean.getUsername(), userBean.getPassword());
        if (isAuthenticated) {
            User authenticatedUser = userService.getUser(userBean.getEmail());
            session.setAttribute("uid", authenticatedUser.getId());
            session.setAttribute("username", authenticatedUser.getUsername());
            log.info("User {} logged in successfully!", authenticatedUser.getUsername());
            return ResponseEntity.ok(UserBean.from(authenticatedUser));
        }
        log.error("Invalid Username or Password! {}", userBean);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new UserBean());
    }

}
