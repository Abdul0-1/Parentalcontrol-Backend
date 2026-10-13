package com.familyshield.security;

import com.familyshield.model.Device;
import com.familyshield.repository.DeviceRepository;
import com.familyshield.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            DeviceRepository deviceRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                var claims = jwtService.claims(token);
                String role = claims.get("role", String.class);
                String subject = claims.getSubject();

                if ("PARENT".equals(role)) {
                    userRepository.findByEmail(subject).ifPresent(user -> {
                        var auth = new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                } else if ("DEVICE".equals(role)) {
                    deviceRepository.findByDeviceUuid(subject).ifPresent(device -> {
                        var auth = new UsernamePasswordAuthenticationToken(
                                subject,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
