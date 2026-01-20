package jomeerkatz.pm.auth_service.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {
    // cryptic key (not a string, hash or something like this)
    // combination of alg, and more stuff
    private final Key secretKey;

    // for example: U2VjcmV0S2V5MTIzNDU2Nzg5YWJjZGVm -> base64 coded -> not the real key
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // [83, 101, 99, 114, 101, 116, 75, 101, 121, 49, 50, 51, ...] -> binary -> bec cryptographic is working with binary
        byte[] keyBites = Base64.getDecoder().decode(secret.getBytes(StandardCharsets.UTF_8));
        // checks if the key is OK and builds the Key Object for example:
        // Key {
        //  algorithm = "HmacSHA256"
        //  encoded = [83, 101, 99, 114, 101, 116, ...]
        //}
        this.secretKey = Keys.hmacShaKeyFor(keyBites); // hmac is a signature algorithm
    }

    public String generateToken(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(secretKey)
                .compact(); // example of the String JWT TOKEN: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb0BleGFtcGxlLmNvbSIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTY5MDAwMDAwMCwiZXhwIjoxNjkwMDM2MDAwfQ.Qx...
                            // signed
    }

    public void validateToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (SignatureException e) {
            throw new JwtException("invalid jwt signature");
        } catch (JwtException e) {
            throw new JwtException("invalid jwt");
        }
    }
}
