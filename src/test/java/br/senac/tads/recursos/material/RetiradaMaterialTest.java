package br.senac.tads.recursos.material;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.suporte.TesteIntegracao;

class RetiradaMaterialTest extends TesteIntegracao {

    private String ana;
    private String responsavel;
    private Recurso sala;
    private Recurso projetor;

    @BeforeEach
    void cenario() {
        ana = token(Papel.SOLICITANTE);
        responsavel = token(Papel.RESPONSAVEL);
        sala = sala("Sala 1");
        projetor = material("Projetor 1");
    }

    private Map<String, Long> projetor() {
        return Map.of("materialId", projetor.getId());
    }

    @Test
    void retiraEDevolveMaterialDaReserva() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala, projetor);

        enviar("/reservas/" + id + "/retiradas", responsavel, projetor())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.material").value("Projetor 1"))
                .andExpect(jsonPath("$.devolvidaEm").doesNotExist());
        enviar("/reservas/" + id + "/devolucoes", responsavel, projetor())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devolvidaEm").exists());

        consultar("/reservas/" + id + "/retiradas", responsavel)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recebidoPor").exists());
    }

    @Test
    void naoRetiraODuasVezesSemDevolver() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), projetor);
        enviar("/reservas/" + id + "/retiradas", responsavel, projetor()).andExpect(status().isCreated());

        enviar("/reservas/" + id + "/retiradas", responsavel, projetor()).andExpect(status().isConflict());
    }

    @Test
    void naoDevolveOQueNaoFoiRetirado() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), projetor);

        enviar("/reservas/" + id + "/devolucoes", responsavel, projetor()).andExpect(status().isConflict());
    }

    @Test
    void soMaterialDaPropriaReserva() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        enviar("/reservas/" + id + "/retiradas", responsavel, projetor()).andExpect(status().isUnprocessableEntity());
        enviar("/reservas/" + id + "/retiradas", responsavel, Map.of("materialId", sala.getId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void reservaAindaNaoAprovadaNaoLiberaMaterial() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), salaRestrita("Lab 1"), projetor);

        enviar("/reservas/" + id + "/retiradas", responsavel, projetor()).andExpect(status().isConflict());
    }

    @Test
    void naoConcluiComMaterialPendente() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala, projetor);
        acao("/reservas/" + id + "/iniciar", responsavel).andExpect(status().isOk());
        enviar("/reservas/" + id + "/retiradas", responsavel, projetor()).andExpect(status().isCreated());

        acao("/reservas/" + id + "/concluir", responsavel)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Há material retirado e não devolvido nesta reserva"));

        enviar("/reservas/" + id + "/devolucoes", responsavel, projetor()).andExpect(status().isOk());
        acao("/reservas/" + id + "/concluir", responsavel).andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    void reservaInexistenteRetorna404() throws Exception {
        enviar("/reservas/999/retiradas", responsavel, projetor()).andExpect(status().isNotFound());
        consultar("/reservas/999/retiradas", responsavel).andExpect(status().isNotFound());
    }

    @Test
    void pedidoSemMaterialRetorna400() throws Exception {
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), projetor);

        enviar("/reservas/" + id + "/retiradas", responsavel, Map.of()).andExpect(status().isBadRequest());
    }
}
