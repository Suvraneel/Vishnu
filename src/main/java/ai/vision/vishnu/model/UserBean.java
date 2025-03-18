package ai.vision.vishnu.model;

import ai.vision.vishnu.entity.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class UserBean {
    private String username;
    private String email;
    private String picture;
    private String password;
    private String role;

    public static UserBean from(User user) {
        UserBean userBean = new UserBean();
        BeanUtils.copyProperties(user, userBean);
        userBean.setPassword(null);
        userBean.setRole(user.getRoleId().name());
        return userBean;
    }
}
