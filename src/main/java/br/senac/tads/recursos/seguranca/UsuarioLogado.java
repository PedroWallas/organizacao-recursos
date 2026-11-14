package br.senac.tads.recursos.seguranca;

import org.springframework.security.oauth2.jwt.Jwt;

/** Dados de quem fez a requisicao, lidos do token. */
public record UsuarioLogado(Long id, String email, Papel papel) {

    public static UsuarioLogado de(Jwt token) {
        Number id = token.getClaim("uid");
        return new UsuarioLogado(id.longValue(), token.getSubject(), Papel.valueOf(token.getClaimAsString("papel")));
    }

    public boolean isGestor() {
        return papel.isGestor();
    }
}
