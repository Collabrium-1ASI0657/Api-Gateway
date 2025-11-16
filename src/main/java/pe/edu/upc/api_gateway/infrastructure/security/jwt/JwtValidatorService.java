package pe.edu.upc.api_gateway.infrastructure.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class JwtValidatorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtValidatorService.class);

  @Value("${authorization.jwt.secret}")
  private String secret;

  private SecretKey getSigningKey() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public boolean validateToken(String token) {
    try {
      // Igual que IAM: parseSignedClaims
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (SignatureException e) {
      LOGGER.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      LOGGER.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      LOGGER.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      LOGGER.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      LOGGER.error("JWT claims string is empty: {}", e.getMessage());
    }
    return false;
  }

  public Map<String, Object> getClaims(String token) {
    Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    return claims;
  }

  public String getUsername(String token) {
    return (String) getClaims(token).get("sub");
  }

  public Long getUserId(String token) {
    Object userId = getClaims(token).get("user_id");
    if (userId instanceof Integer) {
      return ((Integer) userId).longValue();
    } else if (userId instanceof Long) {
      return (Long) userId;
    }
    return null;
  }

  public List<String> getRoles(String token) {
    Object rolesObj = getClaims(token).get("roles"); // usa el nombre exacto del claim en tu JWT
    if (rolesObj instanceof List<?>) {
      // Convertimos cada elemento a String
      return ((List<?>) rolesObj).stream()
          .map(Object::toString)
          .toList();
    }
    return Collections.emptyList();
  }
}