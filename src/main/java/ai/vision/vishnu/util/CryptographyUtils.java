package ai.vision.vishnu.util;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CryptographyUtils {
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }
    
    public String encrypt(String input){
        BCryptPasswordEncoder bCryptPasswordEncoder = bCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(input);
    }
    
    public boolean match(String input, String encrypted){
        BCryptPasswordEncoder bCryptPasswordEncoder = bCryptPasswordEncoder();
        return bCryptPasswordEncoder.matches(input, encrypted);
    }
}
