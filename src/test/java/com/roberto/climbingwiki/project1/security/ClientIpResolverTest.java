package com.roberto.climbingwiki.project1.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver(List.of("127.0.0.1", "::1"));

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
}
