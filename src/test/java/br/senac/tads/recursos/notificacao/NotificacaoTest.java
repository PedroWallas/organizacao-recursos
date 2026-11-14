package br.senac.tads.recursos.notificacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.github.tomakehurst.wiremock.WireMockServer;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.suporte.TesteIntegracao;

/** Integracao externa simulada com WireMock no lugar do servico de avisos. */
class NotificacaoTest extends TesteIntegracao {

    private static final WireMockServer WEBHOOK = new WireMockServer(options().dynamicPort());

    static {
        WEBHOOK.start();
    }

    @DynamicPropertySource
    static void apontarParaWebhook(DynamicPropertyRegistry propriedades) {
        propriedades.add("app.notificacao.url", () -> WEBHOOK.baseUrl() + "/avisos");
    }

    @AfterAll
    static void desligar() {
        WEBHOOK.stop();
    }

    @BeforeEach
    void limparWebhook() {
        WEBHOOK.resetAll();
    }

    @Test
    void avisaCriacaoEAprovacao() throws Exception {
        WEBHOOK.stubFor(post("/avisos").willReturn(aResponse().withStatus(204)));
        Recurso lab = salaRestrita("Lab 1");

        Long id = novaReserva(token(Papel.SOLICITANTE), AMANHA_8H, AMANHA_8H.plusHours(1), lab);
        acao("/reservas/" + id + "/aprovar", token(Papel.RESPONSAVEL)).andExpect(status().isOk());

        WEBHOOK.verify(postRequestedFor(urlEqualTo("/avisos"))
                .withRequestBody(matchingJsonPath("$.novo", equalTo("SOLICITADA"))));
        WEBHOOK.verify(postRequestedFor(urlEqualTo("/avisos"))
                .withRequestBody(matchingJsonPath("$.anterior", equalTo("SOLICITADA")))
                .withRequestBody(matchingJsonPath("$.novo", equalTo("APROVADA"))));
    }

    @Test
    void webhookForaDoArNaoDerrubaAReserva() throws Exception {
        WEBHOOK.stubFor(post("/avisos").willReturn(aResponse().withStatus(503)));

        reservar(token(Papel.SOLICITANTE), AMANHA_8H, AMANHA_8H.plusHours(1), sala("Sala 1"))
                .andExpect(status().isCreated());
    }

    @Test
    void conflitoNaoGeraAviso() throws Exception {
        WEBHOOK.stubFor(post("/avisos").willReturn(aResponse().withStatus(204)));
        Recurso sala = sala("Sala 1");
        String ana = token(Papel.SOLICITANTE);
        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        reservar(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala).andExpect(status().isConflict());

        WEBHOOK.verify(1, postRequestedFor(urlEqualTo("/avisos")));
    }
}
