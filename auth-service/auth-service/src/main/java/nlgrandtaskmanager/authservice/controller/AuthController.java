package nlgrandtaskmanager.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.authservice.controller.dto.LoginRequest;
import nlgrandtaskmanager.authservice.controller.dto.LoginResponse;
import nlgrandtaskmanager.authservice.controller.dto.RegisterRequest;
import nlgrandtaskmanager.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request){
        String token=authService.login(request.email(),request.password());
        return new LoginResponse(token);
    }
    @GetMapping("/me")
    public UUID me(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
