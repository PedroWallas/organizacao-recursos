package br.senac.tads.recursos.recurso;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.suporte.TesteIntegracao;

class RecursoTest extends TesteIntegracao {

    private String admin;
    private String solicitante;

    @BeforeEach
    void tokens() {
        admin = token(Papel.ADMINISTRADOR);
        solicitante = token(Papel.SOLICITANTE);
    }

    @Test
    void administradorCriaAtualizaEExclui() throws Exception {
        Map<String, Object> dados = new HashMap<>(Map.of("nome", "Lab 1", "tipo", "SALA", "capacidade", 25,
                "localizacao", "Bloco C", "restrito", true));
        String corpo = enviar("/recursos", admin, dados)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.restrito").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = lerJson(corpo).get("id").asLong();

        dados.put("emManutencao", true);
        mvc.perform(put("/recursos/" + id).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emManutencao").value(true));

        consultar("/recursos/" + id, solicitante).andExpect(jsonPath("$.nome").value("Lab 1"));

        mvc.perform(delete("/recursos/" + id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        consultar("/recursos/" + id, solicitante).andExpect(status().isNotFound());
    }

    @Test
    void dadosInvalidosRetornam400() throws Exception {
        enviar("/recursos", admin, Map.of("nome", " ", "tipo", "SALA", "capacidade", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(2));
        enviar("/recursos", admin, Map.of("nome", "Sem tipo")).andExpect(status().isBadRequest());
    }

    @Test
    void naoExcluiRecursoQueJaTemReserva() throws Exception {
        Recurso sala = sala("Sala 1");
        novaReserva(solicitante, AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        mvc.perform(delete("/recursos/" + sala.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict());
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        consultar("/recursos/999", solicitante).andExpect(status().isNotFound());
        mvc.perform(put("/recursos/999").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nome", "X", "tipo", "SALA"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void pesquisaPorTipoCapacidadeLocalECompetencia() throws Exception {
        recurso("Auditório", TipoRecurso.SALA, 120, "Bloco A", null, false, false);
        recurso("Sala 12", TipoRecurso.SALA, 30, "Bloco B", null, false, false);
        recurso("Prof. Carla", TipoRecurso.PROFESSOR, null, null, "Banco de Dados", false, false);
        recurso("Prof. Davi", TipoRecurso.PROFESSOR, null, null, "Java e Spring", false, false);
        recurso("Projetor 1", TipoRecurso.MATERIAL, null, "Bloco A", null, false, false);

        consultar("/recursos?tipo=PROFESSOR", solicitante)
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Prof. Carla", "Prof. Davi")));
        consultar("/recursos?capacidadeMinima=50", solicitante)
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Auditório")));
        consultar("/recursos?localizacao=bloco a", solicitante)
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Auditório", "Projetor 1")));
        consultar("/recursos?competencia=java", solicitante)
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Prof. Davi")));
        consultar("/recursos?tipo=SALA&localizacao=&competencia=", solicitante)
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void pesquisaPorDisponibilidadeTiraOcupadosEEmManutencao() throws Exception {
        Recurso ocupada = sala("Ocupada");
        sala("Livre");
        recurso("Quebrada", TipoRecurso.SALA, 10, "Bloco A", null, false, true);
        novaReserva(solicitante, AMANHA_8H, AMANHA_8H.plusHours(2), ocupada);

        String periodo = "livreDe=" + AMANHA_8H.plusHours(1) + "&livreAte=" + AMANHA_8H.plusHours(3);
        consultar("/recursos?tipo=SALA&" + periodo, solicitante)
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Livre")));

        String depois = "livreDe=" + AMANHA_8H.plusHours(2) + "&livreAte=" + AMANHA_8H.plusHours(3);
        consultar("/recursos?tipo=SALA&" + depois, solicitante)
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Livre", "Ocupada")));
    }

    @Test
    void periodoDePesquisaIncompletoOuInvertidoRetorna400() throws Exception {
        consultar("/recursos?livreDe=" + AMANHA_8H, solicitante).andExpect(status().isBadRequest());
        consultar("/recursos?livreDe=" + AMANHA_8H + "&livreAte=" + AMANHA_8H.minusHours(1), solicitante)
                .andExpect(status().isBadRequest());
        consultar("/recursos?tipo=NAVE", solicitante).andExpect(status().isBadRequest());
    }
}
