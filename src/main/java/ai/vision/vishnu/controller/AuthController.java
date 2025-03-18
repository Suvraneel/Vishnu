package ai.vision.vishnu.controller;

import ai.vision.vishnu.model.UserBean;
import jakarta.servlet.http.HttpSession;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthController {
    ResponseEntity<String> register(@RequestBody UserBean userBean) throws BadRequestException;

    ResponseEntity<UserBean> login(@RequestBody UserBean userBean, HttpSession session);
}
