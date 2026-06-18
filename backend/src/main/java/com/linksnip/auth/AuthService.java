package com.linksnip.auth;

import com.linksnip.auth.dto.*;
import com.linksnip.common.BadRequestException;
import com.linksnip.common.ConflictException;
import com.linksnip.common.NotFoundException;
import com.linksnip.security.JwtService;
import com.linksnip.user.User;
import com.linksnip.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenPair register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = new User(email, passwordEncoder.encode(req.password()), req.displayName());
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenPair login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        return issueTokens(user);
    }

    /** Exchange a valid refresh token for a fresh access (+ rotated refresh) token. */
    @Transactional(readOnly = true)
    public TokenPair refresh(RefreshRequest req) {
        final Claims claims;
        try {
            claims = jwtService.parse(req.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadRequestException("Invalid or expired refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadRequestException("Provided token is not a refresh token");
        }
        User user = userRepository.findById(jwtService.userId(claims))
                .orElseThrow(() -> new NotFoundException("User no longer exists"));
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse profile(Long userId) {
        return ProfileResponse.from(getUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = getUser(userId);
        if (req.displayName() != null) {
            user.setDisplayName(req.displayName());
        }
        return ProfileResponse.from(userRepository.save(user));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private TokenPair issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        return TokenPair.of(access, refresh);
    }
}
