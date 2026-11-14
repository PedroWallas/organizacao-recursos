package br.senac.tads.recursos.seguranca;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class SegurancaConfig {

    private static final String RESPONSAVEL = "RESPONSAVEL";
    private static final String ADMINISTRADOR = "ADMINISTRADOR";

    @Bean
    SecretKey chaveJwt(@Value("${app.jwt.segredo}") String segredo) {
        if (segredo.length() < 32) {
            throw new IllegalStateException("app.jwt.segredo precisa ter pelo menos 32 caracteres");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chave) {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Matriz de autorizacao por papel. A posse da reserva e conferida no servico. */
    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter papel = new JwtGrantedAuthoritiesConverter();
        papel.setAuthoritiesClaimName("papel");
        papel.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papel);

        return http
                // API sem sessao nem cookie: o token vai no cabecalho Authorization, que o navegador nao envia
                // sozinho em requisicao forjada por outro site. Por isso CSRF nao se aplica aqui.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR revisado: API stateless com JWT no cabecalho
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/auth/**", "/actuator/health", "/", "/index.html", "/app.js", "/estilo.css",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/recursos/**").authenticated()
                        .requestMatchers("/recursos/**", "/usuarios/**").hasRole(ADMINISTRADOR)
                        .requestMatchers("/relatorios/**", "/reservas/*/retiradas", "/reservas/*/devolucoes")
                        .hasAnyRole(RESPONSAVEL, ADMINISTRADOR)
                        .requestMatchers(HttpMethod.POST, "/reservas/*/aprovar", "/reservas/*/rejeitar",
                                "/reservas/*/iniciar", "/reservas/*/concluir", "/reservas/*/nao-compareceu")
                        .hasAnyRole(RESPONSAVEL, ADMINISTRADOR)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)))
                .build();
    }
}
