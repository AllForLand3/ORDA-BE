package com.allforland.orda.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/** Boot BOM이 관리하는 JOSE 구현으로 HS256 서명·만료를 검증한다. */
@Component
public class JwtTokenService implements JwtDecoder {
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final long validitySeconds;
    private static final String ISSUER = "orda-api";

    public JwtTokenService(
            @Value("${orda.auth.jwt.secret-base64:}") String secret,
            @Value("${orda.auth.jwt.access-token-seconds}") long validitySeconds) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT signing key must be Base64 encoded");
        }
        if (decoded.length < 32 || validitySeconds < 1 || validitySeconds > 3600) {
            throw new IllegalStateException(
                    "JWT requires a key of at least 32 bytes and a lifetime of 1..3600 seconds");
        }
        SecretKey key = new SecretKeySpec(decoded, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.validitySeconds = validitySeconds;
        this.decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ZERO),
                        new JwtIssuerValidator(ISSUER),
                        token -> {
                            Instant issued = token.getIssuedAt();
                            Instant expires = token.getExpiresAt();
                            if (issued == null
                                    || expires == null
                                    || issued.isAfter(Instant.now())
                                    || !expires.isAfter(issued)) {
                                return OAuth2TokenValidatorResult.failure(
                                        new OAuth2Error(
                                                "invalid_token", "Invalid token claims", null));
                            }
                            return OAuth2TokenValidatorResult.success();
                        }));
    }

    public String issue(long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(Long.toString(userId))
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(validitySeconds))
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return decoder.decode(token);
    }
}
