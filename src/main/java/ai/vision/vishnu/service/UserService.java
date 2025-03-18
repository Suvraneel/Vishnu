package ai.vision.vishnu.service;

import ai.vision.vishnu.entity.User;
import ai.vision.vishnu.model.UserBean;
import org.apache.coyote.BadRequestException;

public interface UserService {

//    User registerUser(@AuthenticationPrincipal OidcUser oidcUser);

    User getUser(String email);

    User getUser(Long uid);

    boolean authenticateUser(String email, String username, String password);

    User createUser(UserBean userBean) throws BadRequestException;
}
