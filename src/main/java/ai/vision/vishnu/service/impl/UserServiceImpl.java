package ai.vision.vishnu.service.impl;

import ai.vision.vishnu.entity.User;
import ai.vision.vishnu.enums.Role;
import ai.vision.vishnu.exception.CustomBadRequestException;
import ai.vision.vishnu.model.UserBean;
import ai.vision.vishnu.model.UserPrincipal;
import ai.vision.vishnu.repository.UserRepository;
import ai.vision.vishnu.service.UserService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class UserServiceImpl implements UserDetailsService, UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            log.error("This user does not exist in the database");
            throw new UsernameNotFoundException("This user does not exist in the database");
        }
        return new UserPrincipal(user.get());
    }

    @Override
    public User getUser(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            log.error("User not found: {}", email);
            throw new UsernameNotFoundException(String.format("User not found: %s", email));
        }
        return user.get();
    }

    @Override
    public User getUser(Long uid) {
        Optional<User> user = userRepository.findById(uid);
        if (user.isEmpty()) {
            log.error("User not found for ID: {}", uid);
            throw new UsernameNotFoundException(String.format("User not found for ID: %d", uid));
        }
        return user.get();
    }

    @Override
    public boolean authenticateUser(String email, String username, String password) {
        Optional<User> user = (StringUtils.isNotBlank(email)) ? userRepository.findByEmail(email) : userRepository.findByUsername(username);
        if (user.isEmpty()) {
            log.error("User does not exist: {}", email);
            return false;
        }
        if (bCryptPasswordEncoder.matches(password, user.get().getPasswordHash())) {
            log.info("User authenticated successfully: {}", email);
            return true;
        }
        return false;
    }

    @Override
    public User createUser(UserBean userBean) {
        log.info("Attempting to create user: {}", userBean);
        Optional<User> existingUser = userRepository.findByEmail(userBean.getEmail());
        if (existingUser.isPresent()) {
            log.error("User already exists for email: {}", userBean.getEmail());
            throw new CustomBadRequestException(String.format("User already exists for email: %s", userBean.getEmail()));
        }
        Optional<User> duplicateUser = userRepository.findByUsername(userBean.getUsername());
        if (duplicateUser.isPresent()) {
            log.error("Username is already taken: {}", userBean.getUsername());
            throw new CustomBadRequestException(String.format("Username is already taken: %s", userBean.getUsername()));
        }
        if (!isUsernameValid(userBean.getUsername())) {
            log.error("Username is invalid: {}", userBean.getUsername());
            throw new CustomBadRequestException("Username should be between 5 and 20 characters and can only contain letters, numbers, and underscores");
        }
        User user = new User();
        try {
            user.setUsername(userBean.getUsername());
            user.setEmail(userBean.getEmail());
            user.setRoleId(Role.USER);
            user.setActive(true);
            user.setPasswordHash(bCryptPasswordEncoder.encode(userBean.getPassword()));
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Error creating user: {}", userBean, e);
        }
        log.info("User created successfully: {}", user);
        return user;
    }

    public static boolean isUsernameValid(String username) {
        return username.matches("^[a-zA-Z0-9_]{5,20}$");
    }
//
//    @Override
//    public User registerUser(@AuthenticationPrincipal OidcUser oidcUser) {
//        log.info("Attempting to register user: {}", oidcUser.getEmail());
//        Map<String, Object> claims = oidcUser.getUserInfo().getClaims();
//        Optional<User> optionalUser = Optional.ofNullable(userRepository.findByEmail((String) claims.get("email")));
//        if (optionalUser.isPresent()) {
//            log.info("User already exists: {}", oidcUser.getEmail());
//            return optionalUser.get();
//        }
//        User user = new User();
//        user.setName(claims.get("name").toString());
//        user.setEmail(claims.get("email").toString());
//        user.setPicture(claims.get("picture").toString());
//        user.setRoleId(Role.USER);
//        user.setActive(true);
//        userRepository.save(user);
//        log.info("User created successfully: {}", user);
//        return user;
//    }
}
