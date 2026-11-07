package top.zsj.security.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 签发与解析：4 个用例
 * <p>
 * 只测纯逻辑，不起 Spring、不连数据库。
 *
 * @author zsj
 */
class JwtServiceTest {

    private static final String SECRET =
            "3f2b8d1e7c9a4f6b0d5e8a2c7f1b9d4e6a3c8f2b5d7e9a1c4f6b8d2e5a7c9f1b";

    private static final String OTHER_SECRET =
            "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9c8d7e6f5a4b3c2d1e0f9a8b";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofMinutes(120));

    @Test
    @DisplayName("签发后能解析出用户名与权限列表")
    void shouldParseUsernameAndAuthorities() {
        String token = jwtService.generate("alice", List.of("ROLE_USER", "ARTICLE_DELETE"));

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.get(JwtService.CLAIM_AUTHORITIES, List.class))
                .containsExactlyInAnyOrder("ROLE_USER", "ARTICLE_DELETE");
    }

    @Test
    @DisplayName("篡改签名段第一个字符后验签失败")
    void shouldRejectTamperedSignature() {
        String token = jwtService.generate("alice", List.of("ROLE_USER"));

        // 坑在这里：改最后一个字符可能只改到 Base64url 的填充位，解码后字节没变，验签居然通过。
        // 所以动签名段的第一个字符。
        int signatureStart = token.lastIndexOf('.') + 1;
        char original = token.charAt(signatureStart);
        String tampered = token.substring(0, signatureStart)
                + (original == 'A' ? 'B' : 'A')
                + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("过期 token 解析抛 ExpiredJwtException")
    void shouldRejectExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, Duration.ofSeconds(-1));
        String token = shortLived.generate("alice", List.of("ROLE_USER"));

        assertThatThrownBy(() -> shortLived.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("换一个密钥解析抛 SignatureException")
    void shouldRejectTokenSignedByAnotherKey() {
        String token = jwtService.generate("alice", List.of("ROLE_USER"));
        JwtService another = new JwtService(OTHER_SECRET, Duration.ofMinutes(120));

        assertThatThrownBy(() -> another.parse(token))
                .isInstanceOf(SignatureException.class)
                .isInstanceOf(JwtException.class);
    }
}
