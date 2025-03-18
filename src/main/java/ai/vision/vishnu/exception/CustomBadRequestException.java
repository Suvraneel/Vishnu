package ai.vision.vishnu.exception;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomBadRequestException extends RuntimeException {

    private String message;

    public CustomBadRequestException(String message) {
        super(message);
        this.message = message;
    }

}
