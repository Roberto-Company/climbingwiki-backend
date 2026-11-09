package com.roberto.climbingwiki.security;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Works out the real client IP for per-client limits such as login rate limiting.
 *
 * <p>Two ways to pass on the visitor's IP, both needed so nobody can fake it to dodge the limit:
 *
 * <ul>
 *   <li>{@code X-Client-IP}, honoured only with the right {@code X-Proxy-Secret}. For hosted
 *       setups (site on Vercel, API on Render) where the frontend's address is unknown and the
 *       request reaches the API through the host's own proxy.
 *   <li>{@code X-Forwarded-For}, honoured only when the direct caller is a trusted proxy (the
 *       Next.js server on the same host or Docker network).
 * </ul>
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    static final String CLIENT_IP_HEADER = "X-Client-IP";
    static final String PROXY_SECRET_HEADER = "X-Proxy-Secret";

    private static final int MIN_PROXY_SECRET_LENGTH = 32;
    private static final int MAX_IP_LENGTH = 45;
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]*:[0-9a-fA-F:.]*$");

    private final Set<String> trustedProxies;
    private final byte[] proxySecret;

    public ClientIpResolver(
            @Value("${climbingwiki.security.trusted-proxies:127.0.0.1,::1,0:0:0:0:0:0:0:1}")
                    List<String> trustedProxies,
            @Value("${climbingwiki.security.proxy-secret:}") String proxySecret) {
        this.trustedProxies = Set.copyOf(trustedProxies.stream().map(String::trim).toList());
        this.proxySecret = validatedSecret(proxySecret);
        if (this.proxySecret.length == 0) {
            log.info("PROXY_SHARED_SECRET not set: X-Client-IP is ignored; client IPs come from the"
                    + " direct caller or TRUSTED_PROXIES");
        }
    }

    public String resolve(HttpServletRequest request) {
        String fromSecretProxy = clientIpFromSecretProxy(request);
        if (fromSecretProxy != null) {
            return fromSecretProxy;
        }
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String header = request.getHeader(FORWARDED_FOR_HEADER);
        if (header == null || header.isBlank()) {
            return remoteAddr;
        }
        return rightmostUntrustedEntry(header.split(","), remoteAddr);
    }

    private static byte[] validatedSecret(String secret) {
        String trimmed = secret == null ? "" : secret.trim();
        if (!trimmed.isEmpty() && trimmed.length() < MIN_PROXY_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "PROXY_SHARED_SECRET must be at least " + MIN_PROXY_SECRET_LENGTH + " characters");
        }
        return trimmed.getBytes(StandardCharsets.UTF_8);
    }

    /** The {@code X-Client-IP} value, or null unless the secret is configured and matches. */
    private String clientIpFromSecretProxy(HttpServletRequest request) {
        if (proxySecret.length == 0) {
            return null;
        }
        String sentSecret = request.getHeader(PROXY_SECRET_HEADER);
        if (sentSecret == null
                || !MessageDigest.isEqual(proxySecret, sentSecret.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        String clientIp = request.getHeader(CLIENT_IP_HEADER);
        if (clientIp == null) {
            return null;
        }
        String candidate = clientIp.trim();
        return isPlausibleIp(candidate) ? candidate : null;
    }

    private String rightmostUntrustedEntry(String[] entries, String fallback) {
        for (int i = entries.length - 1; i >= 0; i--) {
            String candidate = entries[i].trim();
            if (trustedProxies.contains(candidate)) {
                continue;
            }
            return isPlausibleIp(candidate) ? candidate : fallback;
        }
        return fallback;
    }

    private static boolean isPlausibleIp(String value) {
        return value.length() <= MAX_IP_LENGTH
                && (IPV4.matcher(value).matches() || IPV6.matcher(value).matches());
    }
}
