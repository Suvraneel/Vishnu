package ai.vision.vishnu.controller;

import ai.vision.vishnu.model.UserBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public interface UserREST {
    @GetMapping("/user/{id}")
    ResponseEntity<UserBean> getUser(@PathVariable Long uid);

    @GetMapping("/user/{email}")
    ResponseEntity<UserBean> getUser(@PathVariable String email);
}
