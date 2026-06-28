package nlgrandtaskmanager.authservice.testcontainers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "spring.datasource.url=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.database-platform=",
        "spring.liquibase.enabled=true"
})
public class AuthIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void register_then_login_returnsToken() {
        var credentials = new HashMap<String, String>();
        credentials.put("email","test1@email.com");
        credentials.put("password","test");

        ResponseEntity<Void> registerResponse = restTemplate.postForEntity(
                "/auth/register",
                credentials,
                Void.class
        );
        assertThat(registerResponse.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<Map>loginResponse=restTemplate.postForEntity(
                "/auth/login",
                credentials,
                Map.class
        );
        assertThat(loginResponse.getStatusCode().is2xxSuccessful()).isTrue();


        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().get("token")).isNotNull();
    }
}
