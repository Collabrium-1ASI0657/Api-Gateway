package pe.edu.upc.api_gateway.infrastructure.security.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements WebFilter {

  private final JwtValidatorService jwtValidatorService;

  public JwtAuthFilter(JwtValidatorService jwtValidatorService) {
    this.jwtValidatorService = jwtValidatorService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    // Rutas públicas que NO requieren JWT
    if (path.startsWith("/iam")) {
      return chain.filter(exchange);
    }

    // Rutas protegidas: Profiles, Authors, etc.
    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }

    String token = authHeader.substring(7);
    if (!jwtValidatorService.validateToken(token)) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }

    // Extraer información del JWT
    String username = jwtValidatorService.getUsername(token);
    Long userId = jwtValidatorService.getUserId(token);
    List<String> roles = jwtValidatorService.getRoles(token);
    String rolesString = String.join(",", roles);

    // Construir nueva request con headers adicionales
    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
        .header("X-User-Id", userId != null ? userId.toString() : "")
        .header("X-Username", username != null ? username : "")
        .header("X-Roles", rolesString)
        .build();

    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

    // Continuar con la cadena
    return chain.filter(mutatedExchange);
  }
}