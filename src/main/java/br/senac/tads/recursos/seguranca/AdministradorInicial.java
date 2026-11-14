package br.senac.tads.recursos.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Cria o primeiro administrador na subida, ja que o cadastro publico so cria solicitantes. */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarios;
    private final UsuarioService service;
    private final String email;
    private final String senha;

    public AdministradorInicial(UsuarioRepository usuarios, UsuarioService service,
            @Value("${app.admin.email:}") String email, @Value("${app.admin.senha:}") String senha) {
        this.usuarios = usuarios;
        this.service = service;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank() || usuarios.existsByEmailIgnoreCase(email)) {
            return;
        }
        service.criar("Administrador", email, senha, Papel.ADMINISTRADOR);
        log.info("Administrador inicial criado: {}", email);
    }
}
