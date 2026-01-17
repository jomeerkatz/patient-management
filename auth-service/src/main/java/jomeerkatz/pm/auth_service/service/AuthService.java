package jomeerkatz.pm.auth_service.service;

import jomeerkatz.pm.auth_service.dto.LoginRequestDTO;
import jomeerkatz.pm.auth_service.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserService userService;

    public AuthService(final UserService userService) {
        this.userService = userService;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail())
                .filter(currentUser -> passwordEncoder.matches(loginRequestDTO.getPassword(),
                        currentUser.getPassword()))
                .map(currentUser -> jwtUtil.generateToken(currentUser.getEmail(), currentUser.getRole()));
        return token;
    }
}
