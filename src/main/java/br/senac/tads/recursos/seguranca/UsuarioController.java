package br.senac.tads.recursos.seguranca;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senac.tads.recursos.seguranca.UsuarioService.Cadastro;
import br.senac.tads.recursos.seguranca.UsuarioService.Login;
import br.senac.tads.recursos.seguranca.UsuarioService.NovoUsuario;
import br.senac.tads.recursos.seguranca.UsuarioService.TokenResposta;
import br.senac.tads.recursos.seguranca.UsuarioService.UsuarioResposta;

@RestController
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResposta cadastrar(@Valid @RequestBody Cadastro dados) {
        return service.cadastrar(dados);
    }

    @PostMapping("/auth/login")
    public TokenResposta login(@Valid @RequestBody Login dados) {
        return service.entrar(dados);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResposta criar(@Valid @RequestBody NovoUsuario dados) {
        return service.criarPorAdministrador(dados);
    }

    @GetMapping("/usuarios")
    public List<UsuarioResposta> listar() {
        return service.listar();
    }
}
