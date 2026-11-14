package br.senac.tads.recursos.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.recurso.RecursoRepository;
import br.senac.tads.recursos.recurso.TipoRecurso;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.seguranca.TokenService;
import br.senac.tads.recursos.seguranca.Usuario;
import br.senac.tads.recursos.seguranca.UsuarioRepository;
import br.senac.tads.recursos.suporte.BancoDeTeste;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTeste.class)
public class AprovacaoPassos {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private RecursoRepository recursos;
    @Autowired
    private PasswordEncoder senhas;
    @Autowired
    private TokenService tokens;

    private Recurso recurso;
    private String tokenSolicitante;
    private long reservaId;
    private MvcResult ultimaResposta;

    @Before
    public void limparBanco() {
        jdbc.execute("TRUNCATE retirada_material, historico_reserva, conflito_evitado, reserva_recurso, reserva, "
                + "recurso, usuario RESTART IDENTITY CASCADE");
    }

    @Dado("o recurso restrito {string}")
    public void oRecursoRestrito(String nome) {
        Recurso novo = new Recurso(nome, TipoRecurso.SALA);
        novo.atualizar(nome, TipoRecurso.SALA, 20, "Bloco C", null, true, false);
        recurso = recursos.save(novo);
    }

    @E("a solicitante {string}")
    public void aSolicitante(String email) {
        tokenSolicitante = tokenDe(email, Papel.SOLICITANTE);
    }

    @Quando("ela reserva {string} amanhã das {int}h às {int}h")
    @Dado("que ela reservou {string} amanhã das {int}h às {int}h")
    public void elaReserva(String nome, int inicio, int fim) throws Exception {
        assertThat(recurso.getNome()).isEqualTo(nome);
        LocalDateTime amanha = LocalDate.now().plusDays(1).atStartOfDay();
        Map<String, Object> pedido = Map.of("recursoIds", List.of(recurso.getId()),
                "inicio", amanha.plusHours(inicio).toString(), "fim", amanha.plusHours(fim).toString());
        ultimaResposta = mvc.perform(post("/reservas").header(HttpHeaders.AUTHORIZATION, tokenSolicitante)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(pedido))).andReturn();
        assertThat(ultimaResposta.getResponse().getStatus()).isEqualTo(201);
        reservaId = json.readTree(ultimaResposta.getResponse().getContentAsString()).get("id").asLong();
    }

    @Quando("o responsável aprova a reserva")
    public void oResponsavelAprova() throws Exception {
        aprovarCom(tokenDe("rui@escola.com", Papel.RESPONSAVEL));
    }

    @Quando("a própria solicitante tenta aprovar a reserva")
    public void aSolicitanteTentaAprovar() throws Exception {
        aprovarCom(tokenSolicitante);
    }

    @Então("a resposta é {int}")
    public void aRespostaE(int status) {
        assertThat(ultimaResposta.getResponse().getStatus()).isEqualTo(status);
    }

    @Então("a reserva fica {string}")
    @E("a reserva continua {string}")
    public void aReservaFica(String status) throws Exception {
        String corpo = mvc.perform(get("/reservas/" + reservaId).header(HttpHeaders.AUTHORIZATION, tokenSolicitante))
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(corpo).get("status").asText()).isEqualTo(status);
    }

    private void aprovarCom(String token) throws Exception {
        ultimaResposta = mvc.perform(post("/reservas/" + reservaId + "/aprovar")
                .header(HttpHeaders.AUTHORIZATION, token)).andReturn();
    }

    private String tokenDe(String email, Papel papel) {
        Usuario usuario = usuarios.save(new Usuario(email, email, senhas.encode("segredo1"), papel));
        return "Bearer " + tokens.gerar(usuario);
    }
}
