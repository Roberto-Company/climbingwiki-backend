package com.roberto.climbingwiki.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1", "::1"), "");

    private static final String SECRET = "proxy-secret-for-tests-0123456789abcdef";

    private final ClientIpResolver resolverWithSecret =
            new ClientIpResolver(List.of("127.0.0.1", "::1"), SECRET);

    private MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void resolve_untrustedCaller_ignoresForwardedHeader() {
        assertThat(resolver.resolve(request("203.0.113.9", "1.1.1.1"))).isEqualTo("203.0.113.9");
    }

    @Test
    void resolve_trustedProxyWithoutHeader_usesRemoteAddr() {
        assertThat(resolver.resolve(request("127.0.0.1", null))).isEqualTo("127.0.0.1");
    }

    @Test
    void resolve_trustedProxy_usesForwardedClientIp() {
        assertThat(resolver.resolve(request("127.0.0.1", "198.51.100.7"))).isEqualTo("198.51.100.7");
    }

    @Test
    void resolve_trustedIpv6Proxy_usesForwardedClientIp() {
        assertThat(resolver.resolve(request("::1", "2001:db8::1"))).isEqualTo("2001:db8::1");
    }

    @Test
    void resolve_chain_usesRightmostEntryThatIsNotATrustedProxy() {
        assertThat(resolver.resolve(request("127.0.0.1", "6.6.6.6, 198.51.100.7, 127.0.0.1")))
                .isEqualTo("198.51.100.7");
    }

    @Test
    void resolve_blankHeader_fallsBackToRemoteAddr() {
        assertThat(resolver.resolve(request("127.0.0.1", "  "))).isEqualTo("127.0.0.1");
    }

    @Test
    void resolve_malformedEntry_fallsBackToRemoteAddr() {
        assertThat(resolver.resolve(request("127.0.0.1", "not-an-ip<script>"))).isEqualTo("127.0.0.1");
    }

    @Test
    void resolve_overlongEntry_fallsBackToRemoteAddr() {
        assertThat(resolver.resolve(request("127.0.0.1", "1".repeat(100)))).isEqualTo("127.0.0.1");
    }

    private MockHttpServletRequest proxiedRequest(String secret, String clientIp) {
        MockHttpServletRequest request = request("10.0.0.5", null);
        if (secret != null) {
            request.addHeader("X-Proxy-Secret", secret);
        }
        if (clientIp != null) {
            request.addHeader("X-Client-IP", clientIp);
        }
        return request;
    }

    @Test
    void resolve_validProxySecret_usesClientIpHeader() {
        assertThat(resolverWithSecret.resolve(proxiedRequest(SECRET, "198.51.100.7"))).isEqualTo("198.51.100.7");
    }

    @Test
    void resolve_wrongProxySecret_ignoresClientIpHeader() {
        assertThat(resolverWithSecret.resolve(proxiedRequest("guess", "198.51.100.7"))).isEqualTo("10.0.0.5");
    }

    @Test
    void resolve_missingProxySecret_ignoresClientIpHeader() {
        assertThat(resolverWithSecret.resolve(proxiedRequest(null, "198.51.100.7"))).isEqualTo("10.0.0.5");
    }

    @Test
    void resolve_noSecretConfigured_ignoresClientIpHeaderEvenWithEmptySecret() {
        assertThat(resolver.resolve(proxiedRequest("", "198.51.100.7"))).isEqualTo("10.0.0.5");
    }

    @Test
    void resolve_validProxySecretButMalformedClientIp_fallsBackToRemoteAddr() {
        assertThat(resolverWithSecret.resolve(proxiedRequest(SECRET, "not-an-ip"))).isEqualTo("10.0.0.5");
    }

    @Test
    void resolve_validProxySecretWithoutClientIp_fallsBackToRemoteAddr() {
        assertThat(resolverWithSecret.resolve(proxiedRequest(SECRET, null))).isEqualTo("10.0.0.5");
    }

    @Test
    void constructor_shortProxySecret_failsAtStartup() {
        assertThatThrownBy(() -> new ClientIpResolver(List.of("127.0.0.1"), "too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROXY_SHARED_SECRET");
    }
}
