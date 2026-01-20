package jomeerkatz.pm.auth_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import jomeerkatz.pm.auth_service.dto.LoginRequestDTO;
import jomeerkatz.pm.auth_service.dto.LoginResponseDTO;
import jomeerkatz.pm.auth_service.service.AuthService;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "validate token") // swagger docs
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader("Authorization") String authHeader) {

        // check, if there isn't Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return authService.validateToken(authHeader.substring(7))
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
