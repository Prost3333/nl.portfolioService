package nlgrandtaskmanager.financesevice.config;

import org.springframework.stereotype.Component;


public class BusinessException extends RuntimeException{

    public BusinessException (String message){
        super(message);
    }
}
