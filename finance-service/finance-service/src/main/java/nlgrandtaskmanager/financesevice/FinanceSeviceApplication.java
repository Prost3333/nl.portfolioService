package nlgrandtaskmanager.financesevice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FinanceSeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceSeviceApplication.class, args);
    }

}
