package br.senac.tads.recursos.comum;

import org.springframework.http.HttpStatus;

/** Erro de regra de negocio que vira uma resposta HTTP com mensagem para o usuario. */
public class ErroApi extends RuntimeException {

    private final HttpStatus status;

    public ErroApi(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ErroApi naoEncontrado(String mensagem) {
        return new ErroApi(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ErroApi conflito(String mensagem) {
        return new ErroApi(HttpStatus.CONFLICT, mensagem);
    }

    public static ErroApi regraViolada(String mensagem) {
        return new ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, mensagem);
    }

    public static ErroApi requisicaoInvalida(String mensagem) {
        return new ErroApi(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ErroApi proibido(String mensagem) {
        return new ErroApi(HttpStatus.FORBIDDEN, mensagem);
    }

    public static ErroApi naoAutorizado(String mensagem) {
        return new ErroApi(HttpStatus.UNAUTHORIZED, mensagem);
    }
}
