package nlgrandtaskmanager.authservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.authservice.model.User;
import nlgrandtaskmanager.authservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final JwtService jwtService;
    @Transactional
    public void register(String email, String password){
        if (userRepository.existsByEmail(email)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
        }
        User user=User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .createAt(Instant.now())
                .build();
        userRepository.save(user);
    }
    public String login(String email, String password){
        User user= userRepository.findByEmail(email)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
        }
        return jwtService.generateToken(user.getId());
    }
}
