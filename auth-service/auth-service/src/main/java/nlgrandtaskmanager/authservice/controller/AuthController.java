package nlgrandtaskmanager.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nlgrandtaskmanager.authservice.controller.dto.LoginResponse;
import nlgrandtaskmanager.authservice.controller.dto.RegisterRequest;
import nlgrandtaskmanager.authservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private  final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.email(), request.password());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody RegisterRequest request){
        String token=authService.login(request.email(),request.password());
        return new LoginResponse(token);
    }
    @GetMapping("/me")
    public UUID me(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
