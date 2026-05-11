package nlgrandtaskmanager.authservice.service;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

public class JwtServiceTest {
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMTIzNDU2Nzg5MA==");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
        ReflectionTestUtils.invokeMethod(jwtService, "init");
    }

    @Test
    void generateToken_thenParseUserId_returnsSameUserId() {
        UUID originalUserId= UUID.randomUUID();

        String token = jwtService.generateToken(originalUserId);
        UUID parsedUserId = jwtService.parseUserId(token);

        assertEquals(originalUserId,parsedUserId);
    }
    @Test
    void generateToken_returnsValidJwtFormat(){
        UUID originalUserId= UUID.randomUUID();
        String token= jwtService.generateToken(originalUserId);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String[] arr=token.split("\\.");
        assertEquals(3,arr.length);
    }
    @Test
    void parseUserId_withInvalidToken_throwsException(){
        UUID originalUserId= UUID.randomUUID();
        String token = jwtService.generateToken(originalUserId)+"error";

        assertThrows(SignatureException.class,()->jwtService.parseUserId(token));

    }
}
