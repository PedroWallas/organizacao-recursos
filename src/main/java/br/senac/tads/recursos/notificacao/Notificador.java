package br.senac.tads.recursos.notificacao;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.senac.tads.recursos.reserva.Eventos.StatusAlterado;

/**
 * Avisa um sistema externo (webhook) a cada mudanca de status de reserva.
 * Roda depois do commit: se o webhook cair, a reserva continua valida e so fica um aviso no log.
 */
@Component
public class Notificador {

    private static final Logger log = LoggerFactory.getLogger(Notificador.class);

    private final RestClient cliente;
    private final String url;

    public Notificador(RestClient.Builder builder, @Value("${app.notificacao.url:}") String url) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(2));
        this.cliente = builder.requestFactory(fabrica).build();
        this.url = url;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void statusAlterado(StatusAlterado evento) {
        if (url.isBlank()) {
            log.info("Notificação simulada: reserva {} {} -> {} para {}", evento.reservaId(), evento.anterior(),
                    evento.novo(), evento.solicitante());
            return;
        }
        try {
            cliente.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(evento).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao notificar reserva {}: {}", evento.reservaId(), e.getMessage());
        }
    }
}
