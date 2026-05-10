package com.rdhxb.smart_building.auth;

import com.rdhxb.smart_building.security.JwtUtil;
import com.rdhxb.smart_building.user.DTO.LogInRequest;
import com.rdhxb.smart_building.user.DTO.RegisterRequest;
import com.rdhxb.smart_building.user.entity.Role;
import com.rdhxb.smart_building.user.entity.User;
import com.rdhxb.smart_building.user.repo.UserRepo;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;
    private UserRepo userRepository;
    private PasswordEncoder encoder;
    private JwtUtil jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserRepo userRepository, PasswordEncoder encoder, JwtUtil jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signin")
    public String authenticateUser(@RequestBody LogInRequest user) {
        Authentication authentication = authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        user.getPassword()
                )
        );

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtUtils.generateToken(userDetails.getUsername());
    }

    @PostMapping("/signup")
    public String registerUser(@Valid @RequestBody RegisterRequest user) {
        if (userRepository.existsUserByUsername(user.getUsername())) {
            return "User already exists!";
        }

        final User newUser = new User(
                null,
                user.getUsername(),
                encoder.encode(user.getPassword()),
                Role.RESIDENT
        );
        userRepository.save(newUser);
        return "User registered successfully!";
    }
}
