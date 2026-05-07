package nlgrandtaskmanager.authservice.service;

import nlgrandtaskmanager.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}