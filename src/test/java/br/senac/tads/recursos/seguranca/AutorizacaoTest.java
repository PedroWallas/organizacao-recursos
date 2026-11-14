package br.senac.tads.recursos.seguranca;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.suporte.TesteIntegracao;

/** Matriz de autorizacao: cada papel so alcanca o que lhe cabe. */
class AutorizacaoTest extends TesteIntegracao {

    @ParameterizedTest(name = "{0} em {1} {2} -> {3}")
    @CsvSource({
            "SOLICITANTE,   POST, /reservas/1/aprovar,       403",
            "SOLICITANTE,   POST, /reservas/1/rejeitar,      403",
            "SOLICITANTE,   POST, /reservas/1/iniciar,       403",
            "SOLICITANTE,   POST, /reservas/1/concluir,      403",
            "SOLICITANTE,   POST, /reservas/1/nao-compareceu,403",
            "SOLICITANTE,   GET,  /reservas/1/retiradas,     403",
            "SOLICITANTE,   GET,  /relatorios/utilizacao,    403",
            "SOLICITANTE,   GET,  /usuarios,                 403",
            "RESPONSAVEL,   GET,  /usuarios,                 403",
            "RESPONSAVEL,   GET,  /relatorios/utilizacao,    200",
            "ADMINISTRADOR, GET,  /relatorios/utilizacao,    200",
            "ADMINISTRADOR, GET,  /usuarios,                 200"
    })
    void papelAlcancaSoOQueLheCabe(Papel papel, String metodo, String url, int esperado) throws Exception {
        String token = token(papel);
        var resposta = "GET".equals(metodo) ? consultar(url, token) : acao(url, token);
        resposta.andExpect(status().is(esperado));
    }

    @Test
    void soAdministradorMexeNoCadastroDeRecursos() throws Exception {
        Map<String, Object> sala = Map.of("nome", "Sala 9", "tipo", "SALA", "capacidade", 10);
        Recurso existente = sala("Sala 1");

        enviar("/recursos", token(Papel.SOLICITANTE), sala).andExpect(status().isForbidden());
        enviar("/recursos", token(Papel.RESPONSAVEL), sala).andExpect(status().isForbidden());
        mvc.perform(delete("/recursos/" + existente.getId())
                .header(HttpHeaders.AUTHORIZATION, token(Papel.RESPONSAVEL)))
                .andExpect(status().isForbidden());
        enviar("/recursos", token(Papel.ADMINISTRADOR), sala).andExpect(status().isCreated());
    }

    @Test
    void solicitanteNaoVeNemMexeNaReservaDeOutro() throws Exception {
        Recurso sala = sala("Sala 1");
        String dono = token(Papel.SOLICITANTE);
        String intruso = token(Papel.SOLICITANTE);
        Long id = novaReserva(dono, AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        consultar("/reservas/" + id, intruso).andExpect(status().isForbidden());
        consultar("/reservas/" + id + "/historico", intruso).andExpect(status().isForbidden());
        acao("/reservas/" + id + "/cancelar", intruso).andExpect(status().isForbidden());
        alterar(id, intruso, AMANHA_8H.plusHours(2), AMANHA_8H.plusHours(3), sala).andExpect(status().isForbidden());

        consultar("/reservas/" + id, dono).andExpect(status().isOk());
        consultar("/reservas/" + id, token(Papel.RESPONSAVEL)).andExpect(status().isOk());
    }

    @Test
    void listagemDoSolicitanteTrazSoAsDele() throws Exception {
        Recurso sala = sala("Sala 1");
        String ana = token(Papel.SOLICITANTE);
        String bia = token(Papel.SOLICITANTE);
        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);
        novaReserva(bia, AMANHA_8H.plusHours(2), AMANHA_8H.plusHours(3), sala);

        consultar("/reservas", ana).andExpect(jsonPath("$", hasSize(1)));
        consultar("/reservas", token(Papel.RESPONSAVEL)).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void responsavelPodeCancelarReservaDeQualquerUm() throws Exception {
        Recurso sala = sala("Sala 1");
        Long id = novaReserva(token(Papel.SOLICITANTE), AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        acao("/reservas/" + id + "/cancelar", token(Papel.RESPONSAVEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }
}
