package br.senac.tads.recursos.comum;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratamentoErros {

    private static final Logger log = LoggerFactory.getLogger(TratamentoErros.class);

    public record RespostaErro(int status, String mensagem, List<String> detalhes) {
    }

    @ExceptionHandler(ErroApi.class)
    ResponseEntity<RespostaErro> erroApi(ErroApi e) {
        return resposta(e.getStatus(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<RespostaErro> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + ": " + campo.getDefaultMessage())
                .sorted()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    ResponseEntity<RespostaErro> formatoInvalido(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição com formato inválido", List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<RespostaErro> edicaoConcorrente(ObjectOptimisticLockingFailureException e) {
        return resposta(HttpStatus.CONFLICT, "O registro foi alterado por outra pessoa. Atualize e tente de novo", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<RespostaErro> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "Operação viola a integridade dos dados", List.of());
    }

    private static ResponseEntity<RespostaErro> resposta(HttpStatus status, String mensagem, List<String> detalhes) {
        return ResponseEntity.status(status).body(new RespostaErro(status.value(), mensagem, detalhes));
    }
}
