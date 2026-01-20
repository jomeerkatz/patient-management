package jomeerkatz.pm.auth_service.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Password;
import jomeerkatz.pm.auth_service.dto.LoginRequestDTO;
import jomeerkatz.pm.auth_service.model.User;
import jomeerkatz.pm.auth_service.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(final UserService userService, final PasswordEncoder passwordEncoder
    , final JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail())
                .filter(currentUser -> passwordEncoder.matches(loginRequestDTO.getPassword(),
                        currentUser.getPassword()))
                .map(currentUser -> jwtUtil.generateToken(currentUser.getEmail(), currentUser.getRole()));
        return token;
    }

    public boolean validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
