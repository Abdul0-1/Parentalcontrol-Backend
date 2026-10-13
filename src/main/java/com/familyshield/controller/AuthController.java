package com.familyshield.controller;

import com.familyshield.dto.*;
import com.familyshield.model.Role;
import com.familyshield.model.User;
import com.familyshield.repository.UserRepository;
import com.familyshield.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email().toLowerCase());
        user.setPassword(encoder.encode(req.password()));
        user.setRole(Role.PARENT);
        users.save(user);

        String token = jwt.createParentToken(user.getEmail(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var userOpt = users.findByEmail(req.email().toLowerCase());

        if (userOpt.isEmpty() || !encoder.matches(req.password(), userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        User user = userOpt.get();
        String token = jwt.createParentToken(user.getEmail(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal principal) {
        return users.findByEmail(principal.getName())
                .map(user -> ResponseEntity.ok(new AuthResponse(null, user.getName(), user.getEmail())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
