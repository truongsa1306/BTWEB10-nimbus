package vn.iotstar.btweb10.services;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.iotstar.btweb10.exception.InvalidJwtException;
import vn.iotstar.btweb10.exception.InvalidJwtException.Reason;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT creation/verification with Nimbus JOSE + JWT. Algorithm is fixed to HS256: a token whose
 * header says anything else (none, HS384, RS256 ...) is rejected before any verification.
 */
@Service
public class JwtService {

    private static final JWSAlgorithm ALGORITHM = JWSAlgorithm.HS256;
    static final String ROLES_CLAIM = "roles";

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    /** Token lifetime in milliseconds, from security.jwt.expiration-time (JWT_EXPIRATION). */
    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    private SecretKey signInKey;

    /** Fail fast on a missing/weak secret instead of failing on first login. */
    @PostConstruct
    void init() {
        if (jwtExpiration <= 0) {
            throw new IllegalStateException("security.jwt.expiration-time must be > 0 (milliseconds)");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretKey);
        } catch (RuntimeException e) {
            throw new IllegalStateException("security.jwt.secret-key (JWT_SECRET) must be a Base64 string", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret-key (JWT_SECRET) must decode to at least 32 bytes for HS256");
        }
        this.signInKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /** Explicit lifetime (ms). Used by tests to mint already-expired tokens. */
    public String generateToken(UserDetails userDetails, long expirationMillis) {
        return buildToken(new HashMap<>(), userDetails, expirationMillis);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        long now = System.currentTimeMillis();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        extraClaims.forEach(claims::claim);
        claims.subject(userDetails.getUsername())
                .issueTime(new Date(now))
                .expirationTime(new Date(now + expiration))
                .claim(ROLES_CLAIM, userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).toList());
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(ALGORITHM).type(JOSEObjectType.JWT).build(),
                    claims.build());
            jwt.sign(new MACSigner(signInKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token); // full verification, throws if invalid
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    /**
     * Parses, checks alg == HS256, verifies the HMAC signature with Nimbus, then checks exp/sub/iat.
     * Every failure is an {@link InvalidJwtException} (mapped to 401).
     */
    private JWTClaimsSet extractAllClaims(String token) {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token); // also rejects "alg":"none" (not a JWS header) and non-3-part input
        } catch (ParseException | RuntimeException e) {
            throw new InvalidJwtException(Reason.MALFORMED, "Malformed JWT", e);
        }
        if (!ALGORITHM.equals(jwt.getHeader().getAlgorithm())) {
            throw new InvalidJwtException(Reason.UNSUPPORTED_ALGORITHM, "Unsupported JWT algorithm");
        }
        try {
            if (!jwt.verify(new MACVerifier(signInKey))) {
                throw new InvalidJwtException(Reason.INVALID_SIGNATURE, "Invalid JWT signature");
            }
        } catch (JOSEException e) {
            throw new InvalidJwtException(Reason.INVALID_SIGNATURE, "Invalid JWT signature", e);
        }
        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new InvalidJwtException(Reason.MALFORMED, "Malformed JWT claims", e);
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpirationTime() == null || claims.getIssueTime() == null) {
            throw new InvalidJwtException(Reason.INVALID_CLAIMS, "Missing required JWT claims");
        }
        if (!claims.getExpirationTime().after(new Date())) {
            throw new InvalidJwtException(Reason.EXPIRED, "Expired JWT");
        }
        return claims;
    }
}
