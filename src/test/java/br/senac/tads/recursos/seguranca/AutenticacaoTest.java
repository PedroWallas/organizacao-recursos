package br.senac.tads.recursos.seguranca;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import br.senac.tads.recursos.suporte.TesteIntegracao;

class AutenticacaoTest extends TesteIntegracao {

    private static final Map<String, String> ANA = Map.of("nome", "Ana", "email", "ana@escola.com", "senha", "segredo1");

    @Test
    void cadastroPublicoSempreCriaSolicitanteMesmoPedindoOutroPapel() throws Exception {
        Map<String, String> tentandoVirarAdmin = Map.of("nome", "Ana", "email", "ana@escola.com", "senha", "segredo1",
                "papel", "ADMINISTRADOR");

        enviar("/auth/cadastro", "", tentandoVirarAdmin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("SOLICITANTE"));
    }

    @Test
    void emailRepetidoRetorna409() throws Exception {
        enviar("/auth/cadastro", "", ANA).andExpect(status().isCreated());

        enviar("/auth/cadastro", "", Map.of("nome", "Outra", "email", "ANA@escola.com", "senha", "segredo2"))
                .andExpect(status().isConflict());
    }

    @Test
    void cadastroInvalidoListaOsCampos() throws Exception {
        enviar("/auth/cadastro", "", Map.of("nome", "", "email", "nao-e-email", "senha", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes", hasSize(3)));
    }

    @Test
    void loginDevolveTokenQueAbreAsRotasProtegidas() throws Exception {
        enviar("/auth/cadastro", "", ANA).andExpect(status().isCreated());

        String corpo = enviar("/auth/login", "", Map.of("email", "ana@escola.com", "senha", "segredo1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("SOLICITANTE"))
                .andReturn().getResponse().getContentAsString();
        String token = lerJson(corpo).get("token").asText();

        consultar("/reservas", "Bearer " + token).andExpect(status().isOk());
    }

    @Test
    void senhaErradaRetorna401() throws Exception {
        enviar("/auth/cadastro", "", ANA).andExpect(status().isCreated());

        enviar("/auth/login", "", Map.of("email", "ana@escola.com", "senha", "errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
    }

    @Test
    void emailInexistenteRetorna401() throws Exception {
        enviar("/auth/login", "", Map.of("email", "ninguem@escola.com", "senha", "segredo1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semTokenOuComTokenAdulteradoRetorna401() throws Exception {
        mvc.perform(get("/reservas")).andExpect(status().isUnauthorized());

        String adulterado = token(Papel.SOLICITANTE) + "x";
        consultar("/reservas", adulterado).andExpect(status().isUnauthorized());
    }

    @Test
    void jsonQuebradoRetorna400() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void administradorCriaUsuarioComPapelEscolhido() throws Exception {
        String admin = token(Papel.ADMINISTRADOR);

        enviar("/usuarios", admin, Map.of("nome", "Rui", "email", "rui@escola.com", "senha", "segredo1",
                "papel", "RESPONSAVEL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("RESPONSAVEL"));

        mvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("rui@escola.com")));
    }

    @Test
    void paginaEDocumentacaoSaoPublicas() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
