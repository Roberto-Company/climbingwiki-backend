package com.roberto.climbingwiki.project1.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Works out the real client IP for per-client limits such as login rate limiting.
 *
 * <p>{@code X-Forwarded-For} is only honoured when the direct caller is a trusted proxy (e.g. the
 * Next.js server); otherwise anyone could pick a fresh fake IP per request and dodge the limit.
 */
@Component
public class ClientIpResolver {

    static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final int MAX_IP_LENGTH = 45;
    private static final Pattern IPV4 = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]*:[0-9a-fA-F:.]*$");

    private final Set<String> trustedProxies;

    public ClientIpResolver(
            @Value("${climbingwiki.security.trusted-proxies:127.0.0.1,::1,0:0:0:0:0:0:0:1}")
                    List<String> trustedProxies) {
        this.trustedProxies = Set.copyOf(trustedProxies.stream().map(String::trim).toList());
    }

    public String resolve(HttpServletRequest request) {
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
