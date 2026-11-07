package com.roberto.climbingwiki.security;

import com.roberto.climbingwiki.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests that carry a bearer token.
 *
 * <p>The token only proves who signed in. The account is looked up on every request so a deleted
 * user is locked out and a role change applies at once, instead of when the token expires. A token
 * that is invalid, expired or belongs to a deleted account gets a 401 even on public endpoints, so
 * the client learns its session is over rather than silently browsing as anonymous.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<AuthenticatedUser> current = currentUser(header.substring(BEARER_PREFIX.length()));
        if (current.isEmpty()) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        AuthenticatedUser user = current.get();
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
        chain.doFilter(request, response);
    }

    /** The account behind the token as it is now, or empty if the token is bad or the account is gone. */
    private Optional<AuthenticatedUser> currentUser(String token) {
        AuthenticatedUser claimed;
        try {
            claimed = jwtService.parse(token);
        } catch (InvalidTokenException ex) {
            return Optional.empty();
        }
        return userRepository.findById(claimed.id())
                .map(user -> new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole()));
    }
}
