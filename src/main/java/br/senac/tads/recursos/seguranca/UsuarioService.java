package br.senac.tads.recursos.seguranca;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senac.tads.recursos.comum.ErroApi;

@Service
public class UsuarioService {

    public record Cadastro(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 6, max = 72) String senha) {
    }

    public record NovoUsuario(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 6, max = 72) String senha,
            @NotNull Papel papel) {
    }

    public record Login(@NotBlank String email, @NotBlank String senha) {
    }

    public record UsuarioResposta(Long id, String nome, String email, Papel papel) {
        static UsuarioResposta de(Usuario u) {
            return new UsuarioResposta(u.getId(), u.getNome(), u.getEmail(), u.getPapel());
        }
    }

    public record TokenResposta(String token, String nome, Papel papel) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder senhas, TokenService tokens) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
    }

    /** Cadastro publico: sempre nasce SOLICITANTE, para ninguem se promover sozinho. */
    @Transactional
    public UsuarioResposta cadastrar(Cadastro dados) {
        return criar(dados.nome(), dados.email(), dados.senha(), Papel.SOLICITANTE);
    }

    @Transactional
    public UsuarioResposta criarPorAdministrador(NovoUsuario dados) {
        return criar(dados.nome(), dados.email(), dados.senha(), dados.papel());
    }

    @Transactional
    public UsuarioResposta criar(String nome, String email, String senha, Papel papel) {
        if (usuarios.existsByEmailIgnoreCase(email)) {
            throw ErroApi.conflito("Já existe usuário com este e-mail");
        }
        Usuario usuario = new Usuario(nome.trim(), email.trim().toLowerCase(), senhas.encode(senha), papel);
        return UsuarioResposta.de(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public TokenResposta entrar(Login dados) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(dados.email().trim())
                .filter(u -> senhas.matches(dados.senha(), u.getSenhaHash()))
                .orElseThrow(() -> ErroApi.naoAutorizado("E-mail ou senha inválidos"));
        return new TokenResposta(tokens.gerar(usuario), usuario.getNome(), usuario.getPapel());
    }

    @Transactional(readOnly = true)
    public List<UsuarioResposta> listar() {
        return usuarios.findAll().stream().map(UsuarioResposta::de).toList();
    }
}
