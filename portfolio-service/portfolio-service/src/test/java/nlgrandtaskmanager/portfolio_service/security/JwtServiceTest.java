package nlgrandtaskmanager.portfolio_service.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "dGVzdF9zZWNyZXRfa2V5X2Zvcl91bml0X3Rlc3RzX29ubHlfMTIzNDU2Nzg5MA==";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        jwtService.init();
    }

    @Test
    void parseUserId_returnsCorrectUuid_forValidToken() {
        UUID expectedId = UUID.randomUUID();
        String token = buildToken(expectedId.toString(), System.currentTimeMillis() + 3_600_000);

        UUID result = jwtService.parseUserId(token);

        assertThat(result).isEqualTo(expectedId);
    }

    @Test
    void parseUserId_throwsJwtException_forMalformedToken() {
        assertThatThrownBy(() -> jwtService.parseUserId("this.is.not.a.valid.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseUserId_throwsJwtException_forExpiredToken() {
        UUID userId = UUID.randomUUID();
        String token = buildToken(userId.toString(), System.currentTimeMillis() - 1_000);

        assertThatThrownBy(() -> jwtService.parseUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseUserId_throwsJwtException_forTokenSignedWithDifferentKey() {
        UUID userId = UUID.randomUUID();
        Key wrongKey = Keys.hmacShaKeyFor(
                "d3JvbmdTZWNyZXRLZXlGb3JUZXN0aW5nUHVycG9zZXNPbmx5MTIz".getBytes());
        String token = Jwts.builder()
                .setSubject(userId.toString())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parseUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseUserId_throwsException_forEmptyToken() {
        assertThatThrownBy(() -> jwtService.parseUserId(""))
                .isInstanceOf(Exception.class);
    }

    private String buildToken(String subject, long expirationMs) {
        return Jwts.builder()
                .setSubject(subject)
                .setExpiration(new Date(expirationMs))
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }
}
