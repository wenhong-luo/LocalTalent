package cn.localtalent.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "localtalent.auth.jwt.secret=oidc-integration-secret-change-me-with-enough-length",
                "localtalent.auth.jwt.ttl-seconds=3600",
                "localtalent.auth.oidc.enabled=true",
                "localtalent.auth.oidc.client-id=localtalent-oidc-test-client",
                "localtalent.auth.oidc.client-secret=oidc-client-secret-test-value",
                "localtalent.auth.oidc.redirect-uri=http://localhost:8080/api/auth/oidc/callback",
                "localtalent.auth.oidc.state-secret=oidc-state-secret-test-value"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
class OidcAuthFlowIT {

    private static final FakeOidcProvider OIDC = FakeOidcProvider.start();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("localtalent_oidc_test")
            .withUsername("localtalent")
            .withPassword("localtalent");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("localtalent.auth.oidc.issuer-uri", OIDC::issuer);
    }

    @Test
    void oidcCandidateShouldMapExistingLocalAccountAndIssueLocalJwt(CapturedOutput output) throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "oidc-candidate-" + suffix + "@example.com";
        jdbcTemplate.update("""
                INSERT INTO candidate_user (mobile, email, password_hash, real_name, register_channel, status)
                VALUES (?, ?, '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'OIDC求职者', 'oidc-test', 1)
                """, "138" + suffix.substring(0, 8), email);

        HttpResponse<String> loginResponse = get("/api/auth/oidc/login?identity_type=candidate&redirect=/candidate/center");
        assertThat(loginResponse.statusCode()).isEqualTo(302);
        String location = loginResponse.headers().firstValue("Location").orElseThrow();
        String state = queryParam(location, "state");
        String nonce = decodeStateNonce(state);
        OIDC.nextToken("candidate-subject-" + suffix, email, true, nonce);

        HttpResponse<String> callbackResponse = get("/api/auth/oidc/callback?code=valid-code&state=" + encode(state));
        assertThat(callbackResponse.statusCode()).isEqualTo(200);
        assertThat(callbackResponse.body()).contains("localtalent_access_token");
        assertThat(callbackResponse.body()).contains("/candidate/center");
        assertThat(callbackResponse.body()).doesNotContain(email);
        assertThat(callbackResponse.body()).doesNotContain("provider-access-token");

        String localJwt = callbackResponse.body().replaceAll("(?s).*localtalent_access_token',\\s*\"([^\"]+)\".*", "$1");
        HttpResponse<String> meResponse = get("/api/auth/me", "Bearer " + localJwt);
        JsonNode meJson = OBJECT_MAPPER.readTree(meResponse.body());
        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meJson.at("/data/identity_type").asText()).isEqualTo("candidate");
        assertThat(meJson.at("/data/display_name").asText()).isEqualTo("OIDC求职者");

        Integer linkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_oidc_identity_link WHERE issuer = ? AND identity_type = 'candidate'",
                Integer.class,
                OIDC.issuer());
        assertThat(linkCount).isPositive();
        assertThat(output).doesNotContain("provider-access-token")
                .doesNotContain("oidc-client-secret-test-value")
                .doesNotContain(email);
    }

    @Test
    void oidcCandidateShouldIgnoreRedirectForDifferentIdentitySection() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "oidc-candidate-redirect-" + suffix + "@example.com";
        jdbcTemplate.update("""
                INSERT INTO candidate_user (mobile, email, password_hash, real_name, register_channel, status)
                VALUES (?, ?, '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'OIDC求职者', 'oidc-test', 1)
                """, "137" + suffix.substring(0, 8), email);

        HttpResponse<String> loginResponse = get("/api/auth/oidc/login?identity_type=candidate&redirect=/admin");
        String state = queryParam(loginResponse.headers().firstValue("Location").orElseThrow(), "state");
        OIDC.nextToken("candidate-redirect-subject-" + suffix, email, true, decodeStateNonce(state));

        HttpResponse<String> callbackResponse = get("/api/auth/oidc/callback?code=valid-code&state=" + encode(state));

        assertThat(callbackResponse.statusCode()).isEqualTo(200);
        assertThat(callbackResponse.body()).contains("/candidate/center");
        assertThat(callbackResponse.body()).doesNotContain("window.location.replace(\"/admin\")");
    }

    @Test
    void oidcCompanyShouldMapExistingLocalAccountAndRedirectToCompanyCenter() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String email = "oidc-company-" + suffix + "@example.com";
        String licenseNo = "OIDC-LIC-" + suffix.substring(0, 16);
        jdbcTemplate.update("""
                INSERT INTO company (company_name, license_no, industry_code, scale_code, city_code, auth_status, source_system)
                VALUES (?, ?, 'software', '50-100', '310000', 2, 'oidc-test')
                """, "OIDC企业" + suffix.substring(0, 6), licenseNo);
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT id FROM company WHERE license_no = ?",
                Long.class,
                licenseNo);
        jdbcTemplate.update("""
                INSERT INTO company_user (company_id, user_name, role_code, mobile, email, password_hash, status)
                VALUES (?, 'OIDC企业管理员', 'company_admin', ?, ?, '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 1)
                """, companyId, "136" + suffix.substring(0, 8), email);

        HttpResponse<String> loginResponse = get("/api/auth/oidc/login?identity_type=company&redirect=/company");
        String state = queryParam(loginResponse.headers().firstValue("Location").orElseThrow(), "state");
        OIDC.nextToken("company-subject-" + suffix, email, true, decodeStateNonce(state));

        HttpResponse<String> callbackResponse = get("/api/auth/oidc/callback?code=valid-code&state=" + encode(state));
        String localJwt = localJwtFrom(callbackResponse);
        HttpResponse<String> meResponse = get("/api/auth/me", "Bearer " + localJwt);
        JsonNode meJson = OBJECT_MAPPER.readTree(meResponse.body());

        assertThat(callbackResponse.statusCode()).isEqualTo(200);
        assertThat(callbackResponse.body()).contains("/company");
        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meJson.at("/data/identity_type").asText()).isEqualTo("company");
        assertThat(meJson.at("/data/company_id").asLong()).isEqualTo(companyId);
    }

    @Test
    void oidcOperatorShouldMapExistingLocalAdminWithoutTrustingProviderRoleClaim() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = "oidc_operator_" + suffix.substring(0, 12);
        String email = username + "@example.com";
        jdbcTemplate.update("""
                INSERT INTO admin_user (username, display_name, email, password_hash, role_code, status)
                VALUES (?, 'OIDC运营', ?, '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'operator', 1)
                """, username, email);

        HttpResponse<String> loginResponse = get("/api/auth/oidc/login?identity_type=operator&redirect=/admin");
        String state = queryParam(loginResponse.headers().firstValue("Location").orElseThrow(), "state");
        OIDC.nextToken("operator-subject-" + suffix, email, true, decodeStateNonce(state));

        HttpResponse<String> callbackResponse = get("/api/auth/oidc/callback?code=valid-code&state=" + encode(state));
        String localJwt = localJwtFrom(callbackResponse);
        HttpResponse<String> meResponse = get("/api/auth/me", "Bearer " + localJwt);
        JsonNode meJson = OBJECT_MAPPER.readTree(meResponse.body());

        assertThat(callbackResponse.statusCode()).isEqualTo(200);
        assertThat(callbackResponse.body()).contains("/admin");
        assertThat(callbackResponse.body()).contains("localtalent_admin_role_hint");
        assertThat(meResponse.statusCode()).isEqualTo(200);
        assertThat(meJson.at("/data/identity_type").asText()).isEqualTo("operator");
        assertThat(meJson.at("/data/role_codes").toString()).contains("operator");
    }

    @Test
    void oidcShouldRejectInvalidStateWithoutLeakingProviderTokens() throws Exception {
        HttpResponse<String> callbackResponse = get("/api/auth/oidc/callback?code=valid-code&state=invalid-state");

        assertThat(callbackResponse.statusCode()).isEqualTo(302);
        String location = callbackResponse.headers().firstValue("Location").orElseThrow();
        assertThat(location).contains("/auth/login?oidc_error=AUTH_401");
        assertThat(location).doesNotContain("id_token")
                .doesNotContain("access_token")
                .doesNotContain("claims");
    }

    @Test
    void oidcShouldRejectUnverifiedEmail() throws Exception {
        String email = "oidc-unverified-" + UUID.randomUUID() + "@example.com";
        HttpResponse<String> loginResponse = get("/api/auth/oidc/login?identity_type=candidate&redirect=/candidate/center");
        String state = queryParam(loginResponse.headers().firstValue("Location").orElseThrow(), "state");
        OIDC.nextToken("candidate-unverified", email, false, decodeStateNonce(state));

        HttpResponse<String> callbackResponse = get("/api/auth/oidc/callback?code=valid-code&state=" + encode(state));

        assertThat(callbackResponse.statusCode()).isEqualTo(302);
        assertThat(callbackResponse.headers().firstValue("Location").orElseThrow()).contains("oidc_error=AUTH_401");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return get(path, null);
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Trace-Id", "trace-oidc-test")
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String localJwtFrom(HttpResponse<String> response) {
        assertThat(response.body()).contains("localtalent_access_token");
        return response.body().replaceAll("(?s).*localtalent_access_token',\\s*\"([^\"]+)\".*", "$1");
    }

    private static String decodeStateNonce(String state) throws Exception {
        String payload = new String(Base64.getUrlDecoder().decode(state.split("\\.")[0]), StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readTree(payload).path("nonce").asText();
    }

    private static String queryParam(String uri, String name) {
        String query = URI.create(uri).getRawQuery();
        for (String part : query.split("&")) {
            String[] pieces = part.split("=", 2);
            if (pieces.length == 2 && name.equals(URLDecoder.decode(pieces[0], StandardCharsets.UTF_8))) {
                return URLDecoder.decode(pieces[1], StandardCharsets.UTF_8);
            }
        }
        throw new IllegalArgumentException("missing query parameter " + name);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class FakeOidcProvider {
        private static final String KID = "localtalent-test-key";

        private final HttpServer server;
        private final KeyPair keyPair;
        private final AtomicReference<TokenClaims> nextClaims = new AtomicReference<>();

        private FakeOidcProvider(HttpServer server, KeyPair keyPair) {
            this.server = server;
            this.keyPair = keyPair;
        }

        static FakeOidcProvider start() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                FakeOidcProvider provider = new FakeOidcProvider(server, keyPair);
                server.createContext("/.well-known/openid-configuration", provider::discovery);
                server.createContext("/jwks", provider::jwks);
                server.createContext("/token", provider::token);
                server.setExecutor(Executors.newSingleThreadExecutor());
                server.start();
                return provider;
            } catch (Exception exception) {
                throw new IllegalStateException("failed to start fake oidc provider", exception);
            }
        }

        String issuer() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void nextToken(String subject, String email, boolean emailVerified, String nonce) {
            nextClaims.set(new TokenClaims(subject, email, emailVerified, nonce));
        }

        private void discovery(HttpExchange exchange) throws IOException {
            writeJson(exchange, Map.of(
                    "issuer", issuer(),
                    "authorization_endpoint", issuer() + "/authorize",
                    "token_endpoint", issuer() + "/token",
                    "jwks_uri", issuer() + "/jwks"));
        }

        private void jwks(HttpExchange exchange) throws IOException {
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            writeJson(exchange, Map.of("keys", List.of(Map.of(
                    "kty", "RSA",
                    "kid", KID,
                    "alg", "RS256",
                    "use", "sig",
                    "n", base64Url(unsigned(publicKey.getModulus())),
                    "e", base64Url(unsigned(publicKey.getPublicExponent()))))));
        }

        private void token(HttpExchange exchange) throws IOException {
            TokenClaims claims = nextClaims.get();
            if (claims == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            try {
                writeJson(exchange, Map.of(
                        "id_token", idToken(claims),
                        "access_token", "provider-access-token",
                        "token_type", "Bearer",
                        "expires_in", 300));
            } catch (Exception exception) {
                throw new IOException(exception);
            }
        }

        private String idToken(TokenClaims tokenClaims) throws Exception {
            String header = base64Url(OBJECT_MAPPER.writeValueAsBytes(Map.of(
                    "alg", "RS256",
                    "typ", "JWT",
                    "kid", KID)));
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", issuer());
            claims.put("aud", "localtalent-oidc-test-client");
            claims.put("sub", tokenClaims.subject());
            claims.put("email", tokenClaims.email());
            claims.put("email_verified", tokenClaims.emailVerified());
            claims.put("nonce", tokenClaims.nonce());
            claims.put("iat", Instant.now().getEpochSecond());
            claims.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
            String payload = base64Url(OBJECT_MAPPER.writeValueAsBytes(claims));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign((RSAPrivateKey) keyPair.getPrivate());
            signature.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
            return header + "." + payload + "." + base64Url(signature.sign());
        }

        private static void writeJson(HttpExchange exchange, Object body) throws IOException {
            byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(body);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private static byte[] unsigned(BigInteger value) {
            byte[] bytes = value.toByteArray();
            if (bytes.length > 1 && bytes[0] == 0) {
                return java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
            }
            return bytes;
        }

        private static String base64Url(byte[] bytes) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        private record TokenClaims(String subject, String email, boolean emailVerified, String nonce) {
        }
    }
}
