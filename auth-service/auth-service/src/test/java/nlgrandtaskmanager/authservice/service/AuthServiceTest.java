package nlgrandtaskmanager.authservice.service;

import nlgrandtaskmanager.authservice.model.User;
import nlgrandtaskmanager.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_whenEmailAlreadyExists_throwsException() {
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> authService.register(email, "password123")
        );

        assertEquals("Email already exists", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenEmailIsNew_savesUserWithHashedPassword() {
        String email = "new@example.com";
        String rawPassword = "password123";
        String hashedPassword = "hashed_password_xyz";

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        authService.register(email, rawPassword);

        verify(userRepository).save(argThat(user ->
            user.getEmail().equals(email) &&
            user.getPasswordHash().equals(hashedPassword) &&
            user.getCreateAt() != null
        ));
    }

    @Test
    void login_whenUserNotFound(){
        String email = "missing@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> authService.login(email, password)
        );
        assertEquals("Invalid credentials", ex.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_whenPasswordIsWrong_throwsException(){
        String email = "user@example.com";
        String password = "password123";
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("password")
                .createAt(Instant.now())
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password,user.getPasswordHash())).thenReturn(false);
        IllegalStateException ex=assertThrows(
                IllegalStateException.class,
                ()-> authService.login(email,password));

        assertEquals("Invalid credentials",ex.getMessage());
        verify(jwtService,never()).generateToken(any());

    }
    @Test
    void login_whenCredentialsAreCorrect_returnsToken(){
        String email = "user@example.com";
        String password = "password123";
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("password123")
                .createAt(Instant.now())
                .build();


        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password,user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user.getId())).thenReturn("fake-jwt-token-123");

        String result= authService.login(email, password);
        assertEquals("fake-jwt-token-123", result);

        verify(jwtService).generateToken(user.getId());

    }
}