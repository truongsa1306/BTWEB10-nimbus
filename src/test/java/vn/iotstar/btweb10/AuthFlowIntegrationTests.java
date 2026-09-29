package vn.iotstar.btweb10;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.btweb10.entity.User;
import vn.iotstar.btweb10.repository.UserRepository;
import vn.iotstar.btweb10.services.JwtService;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    private static String json(String email, String pw, String name) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + pw + "\",\"fullName\":\"" + name + "\"}";
    }

    private void signup(String email, String pw) throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json(email, pw, "Test User")))
                .andExpect(status().isCreated());
    }

    private String login(String email, String pw) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}"))
                .andExpect(status().isOk()).andReturn();
        Matcher m = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(r.getResponse().getContentAsString());
        assertTrue(m.find(), "token in response");
        return m.group(1);
    }

    @Test
    void signupSucceedsStoresBcryptAndHidesPassword() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json("a@x.com", "secret1", "A")))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(containsString("ROLE_USER")));
        User saved = users.findByEmail("a@x.com").orElseThrow();
        assertTrue(saved.getPassword().startsWith("$2"), "BCrypt hash");
        assertFalse(saved.getPassword().contains("secret1"));
    }

    @Test
    void signupDuplicateEmailIs409() throws Exception {
        signup("dup@x.com", "secret1");
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json("DUP@x.com", "secret1", "B")))
                .andExpect(status().isConflict());
    }

    @Test
    void signupInvalidBodyIs400() throws Exception {
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json("not-an-email", "1", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginValidReturnsTokenAndExpiresIn() throws Exception {
        signup("l@x.com", "secret1");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"l@x.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"expiresIn\":3600000")));
    }

    @Test
    void tokenExpiryMatchesConfiguration() throws Exception {
        signup("e@x.com", "secret1");
        String token = login("e@x.com", "secret1");
        long life = jwtService.extractExpiration(token).getTime()
                - jwtService.extractClaim(token, c -> c.getIssueTime()).getTime();
        // JWT dates have 1s resolution
        assertTrue(Math.abs(life - jwtService.getExpirationTime()) <= 1000, "life=" + life);
    }

    @Test
    void loginWrongPasswordIs401() throws Exception {
        signup("w@x.com", "secret1");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"w@x.com\",\"password\":\"nope-nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUnknownUserIs401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ghost@x.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockedAccountLoginIs403() throws Exception {
        signup("lock@x.com", "secret1");
        User u = users.findByEmail("lock@x.com").orElseThrow();
        u.setLocked(true);
        users.save(u);
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lock@x.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedWithoutTokenIs401() throws Exception {
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("\"status\":401")));
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenGetsMeAndUsersWithoutPassword() throws Exception {
        signup("me@x.com", "secret1");
        String token = login("me@x.com", "secret1");
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("me@x.com")))
                .andExpect(content().string(not(containsString("password"))));
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("me@x.com")))
                .andExpect(content().string(not(containsString("password"))));
    }

    private static String forge(JWSAlgorithm alg, byte[] key) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("s@x.com")
                .issueTime(new Date()).expirationTime(new Date(System.currentTimeMillis() + 60_000)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(alg), claims);
        jwt.sign(new MACSigner(key));
        return jwt.serialize();
    }

    @Test
    void wrongSignatureIs401() throws Exception {
        signup("s@x.com", "secret1");
        String forged = forge(JWSAlgorithm.HS256, "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("signature")));
    }

    @Test
    void unsupportedAlgorithmHs512Is401() throws Exception {
        signup("s@x.com", "secret1");
        // HS512 needs a >= 64 byte key to sign; the point is that the server rejects alg != HS256
        String forged = forge(JWSAlgorithm.HS512,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("algorithm")));
    }

    @Test
    void algNoneTokenIs401() throws Exception {
        signup("s@x.com", "secret1");
        java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = enc.encodeToString(("{\"sub\":\"s@x.com\",\"exp\":"
                + (System.currentTimeMillis() / 1000 + 600) + "}").getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + header + "." + payload + "."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generatedTokenHasThreePartsHs256AndClaims() throws Exception {
        signup("g@x.com", "secret1");
        String token = login("g@x.com", "secret1");
        assertEquals(3, token.split("\\.").length);
        SignedJWT jwt = SignedJWT.parse(token);
        assertEquals(JWSAlgorithm.HS256, jwt.getHeader().getAlgorithm());
        assertEquals("JWT", jwt.getHeader().getType().getType());
        assertEquals("g@x.com", jwt.getJWTClaimsSet().getSubject());
        assertTrue(jwt.getJWTClaimsSet().getExpirationTime() != null);
        assertEquals(java.util.List.of("ROLE_USER"), jwt.getJWTClaimsSet().getStringListClaim("roles"));
    }

    @Test
    void malformedTokenIs401() throws Exception {
        mvc.perform(get("/users/me").header("Authorization", "Bearer dfdfdfdfd"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/users/me").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        signup("x@x.com", "secret1");
        User u = users.findByEmail("x@x.com").orElseThrow();
        String expired = jwtService.generateToken(u, -60_000);
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("expired")));
    }

    @Test
    void tokenOfDeletedUserIs401() throws Exception {
        signup("d@x.com", "secret1");
        String token = login("d@x.com", "secret1");
        users.deleteAll();
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userWithoutRequiredRoleIs403() throws Exception {
        User u = new User();
        u.setFullName("Guest");
        u.setEmail("guest@x.com");
        u.setPassword(encoder.encode("secret1"));
        u.setRole("ROLE_GUEST");
        users.save(u);
        String token = login("guest@x.com", "secret1");
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"status\":403")));
    }

    @Test
    void staleTokenDoesNotBlockLogin() throws Exception {
        signup("st@x.com", "secret1");
        mvc.perform(post("/auth/login").header("Authorization", "Bearer garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"st@x.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void unknownPathWithoutTokenIs401NotPublic() throws Exception {
        assertEquals(401, mvc.perform(get("/api/anything")).andReturn().getResponse().getStatus());
    }
}
