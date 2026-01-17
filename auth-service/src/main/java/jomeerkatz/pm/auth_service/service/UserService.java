package jomeerkatz.pm.auth_service.service;

import jomeerkatz.pm.auth_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import jomeerkatz.pm.auth_service.model.User;

import java.util.Optional;

@Service
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
       return userRepository.findByEmail(email);
    }
}
