package dev.financas.FinancasSpring.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-for-jwt";
    private static final long EXPIRATION = 3600000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void deveGerarTokenComClaimsCorretos() {
        String token = jwtUtil.generateToken(1L, "user@email.com", "USER");
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@email.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void deveValidarTokenCorretamente() {
        String token = jwtUtil.generateToken(1L, "user@email.com", "USER");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void deveRejeitarTokenMalformado() {
        assertThat(jwtUtil.validateToken("token.invalido.aqui")).isFalse();
    }

    @Test
    void deveRejeitarTokenVazio() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void deveRejeitarTokenExpirado() {
        // Cria JwtUtil com expiração de 0ms (já expirado ao gerar)
        JwtUtil expiradoUtil = new JwtUtil(SECRET, 0L);
        String token = expiradoUtil.generateToken(1L, "user@email.com", "USER");

        // Aguarda um pouco para garantir expiração
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(expiradoUtil.validateToken(token)).isFalse();
    }

    @Test
    void deveLancarExcecaoComSecretCurto() {
        assertThrows(IllegalArgumentException.class, () -> {
            new JwtUtil("curto", 3600000L);
        });
    }

    @Test
    void deveExtrairUsernameDoToken() {
        String token = jwtUtil.generateToken(42L, "admin@financas.dev", "ADMIN");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin@financas.dev");
    }

    @Test
    void deveExtrairRoleAdmin() {
        String token = jwtUtil.generateToken(1L, "admin@email.com", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }
}
