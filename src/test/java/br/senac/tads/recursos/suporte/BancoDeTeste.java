package br.senac.tads.recursos.suporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/** PostgreSQL real em container, o mesmo banco de producao. */
@TestConfiguration(proxyBeanMethods = false)
public class BancoDeTeste {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }
}
