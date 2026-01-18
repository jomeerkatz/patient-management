package jomeerkatz.pm.auth_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import jomeerkatz.pm.auth_service.dto.LoginRequestDTO;
import jomeerkatz.pm.auth_service.dto.LoginResponseDTO;
import jomeerkatz.pm.auth_service.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        Optional<String> tokenOption = authService.authenticate(loginRequestDTO);

        if (tokenOption.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenOption.get();

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
}
