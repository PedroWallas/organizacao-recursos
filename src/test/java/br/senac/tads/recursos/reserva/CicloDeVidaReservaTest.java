package br.senac.tads.recursos.reserva;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.seguranca.Usuario;
import br.senac.tads.recursos.suporte.TesteIntegracao;

/** Maquina de estados pela API, com o historico auditavel de cada passo. */
class CicloDeVidaReservaTest extends TesteIntegracao {

    private String ana;
    private Usuario rui;
    private String responsavel;

    @BeforeEach
    void pessoas() {
        ana = token(Papel.SOLICITANTE);
        rui = usuario(Papel.RESPONSAVEL);
        responsavel = token(rui);
    }

    @Test
    void fluxoCompletoFicaRegistradoNoHistorico() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), salaRestrita("Lab 1"));

        acao("/reservas/" + id + "/aprovar", responsavel).andExpect(jsonPath("$.status").value("APROVADA"));
        acao("/reservas/" + id + "/iniciar", responsavel).andExpect(jsonPath("$.status").value("EM_USO"));
        acao("/reservas/" + id + "/concluir", responsavel).andExpect(jsonPath("$.status").value("CONCLUIDA"));

        consultar("/reservas/" + id + "/historico", ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].acao").value("CRIACAO"))
                .andExpect(jsonPath("$[0].de").doesNotExist())
                .andExpect(jsonPath("$[0].para").value("SOLICITADA"))
                .andExpect(jsonPath("$[1].de").value("SOLICITADA"))
                .andExpect(jsonPath("$[1].para").value("APROVADA"))
                .andExpect(jsonPath("$[1].autor").value(rui.getEmail()))
                .andExpect(jsonPath("$[3].para").value("CONCLUIDA"));
    }

    @Test
    void responsavelRejeita() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), salaRestrita("Lab 1"));

        acao("/reservas/" + id + "/rejeitar", responsavel).andExpect(jsonPath("$.status").value("REJEITADA"));
        acao("/reservas/" + id + "/aprovar", responsavel).andExpect(status().isConflict());
    }

    @Test
    void registraNaoComparecimento() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala("Sala 1"));

        acao("/reservas/" + id + "/nao-compareceu", responsavel)
                .andExpect(jsonPath("$.status").value("NAO_COMPARECEU"));
    }

    @Test
    void reservaEmUsoNaoPodeSerCancelada() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala("Sala 1"));
        acao("/reservas/" + id + "/iniciar", responsavel).andExpect(status().isOk());

        acao("/reservas/" + id + "/cancelar", ana)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Não é possível passar a reserva de EM_USO para CANCELADA"));
    }

    @Test
    void naoPulaEtapas() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), salaRestrita("Lab 1"));

        acao("/reservas/" + id + "/iniciar", responsavel).andExpect(status().isConflict());
        acao("/reservas/" + id + "/concluir", responsavel).andExpect(status().isConflict());
    }

    @Test
    void aprovarDuasVezesRetorna409() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), salaRestrita("Lab 1"));
        acao("/reservas/" + id + "/aprovar", responsavel).andExpect(status().isOk());

        acao("/reservas/" + id + "/aprovar", responsavel).andExpect(status().isConflict());
    }

    @Test
    void administradorTambemAprova() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), salaRestrita("Lab 1"));

        acao("/reservas/" + id + "/aprovar", token(Papel.ADMINISTRADOR))
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }
}
