package ai.vision.vishnu.config;

import ai.vision.vishnu.entity.User;
import ai.vision.vishnu.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${okta.oauth2.issuer}")
    private String issuer;
    @Value("${okta.oauth2.client-id}")
    private String clientId;
    @Value("${ui.base.url}")
    private String uibaseUrl;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/*").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        return provider;
    }
//                .cors(Customizer.withDefaults())
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers("/", "/images/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .oauth2Login(oauth2 -> oauth2
//                        .successHandler((request, response, authentication) -> {
//                            try {
//                                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
//                                User loggedInUser = userService.registerUser(oidcUser);
//                                response.addHeader("uid", loggedInUser.getId().toString());
//                                response.sendRedirect(uibaseUrl);
//                                log.info("User {} successfully logged in", loggedInUser.getName());
//                            } catch (Exception e) {
//                                log.error("User registration failed: {}", e.getMessage());
//                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed");
//                            }
//                        })
//                )
//                .logout(logout -> logout
//                        .addLogoutHandler(logoutHandler()).logoutSuccessHandler(
//                                (request, response, authentication) -> {
//                                    log.info("User successfully logged out");
//                                }
//                        ));
//        return http.build();

//    private LogoutHandler logoutHandler() {
//        return (request, response, authentication) -> {
//            try {
//                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
//                response.sendRedirect(issuer + "v2/logout?client_id=" + clientId + "&returnTo=" + uibaseUrl);
//                if (authentication != null) {
//                    SecurityContextHolder.clearContext(); // Clears security context
//                    request.getSession().invalidate(); // Invalidates session
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        };
//    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*"); // Allow all origins
        config.addAllowedHeader("*"); // Allow all headers
        config.addAllowedMethod("*"); // Allow all methods
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
