package br.senac.tads.recursos.suporte;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.recurso.RecursoRepository;
import br.senac.tads.recursos.recurso.TipoRecurso;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.seguranca.TokenService;
import br.senac.tads.recursos.seguranca.Usuario;
import br.senac.tads.recursos.seguranca.UsuarioLogado;
import br.senac.tads.recursos.seguranca.UsuarioRepository;

/** Base dos testes de integracao: aplicacao inteira + PostgreSQL em container, banco limpo a cada teste. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoDeTeste.class)
public abstract class TesteIntegracao {

    protected static final LocalDateTime AMANHA_8H = LocalDate.now().plusDays(1).atTime(8, 0);

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected UsuarioRepository usuarios;
    @Autowired
    protected RecursoRepository recursos;
    @Autowired
    private PasswordEncoder senhas;
    @Autowired
    private TokenService tokens;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE retirada_material, historico_reserva, conflito_evitado, reserva_recurso, reserva, "
                + "recurso, usuario RESTART IDENTITY CASCADE");
    }

    protected Usuario usuario(Papel papel) {
        String email = papel.name().toLowerCase() + SEQUENCIA.incrementAndGet() + "@teste.com";
        return usuarios.save(new Usuario("Usuário " + papel, email, senhas.encode("segredo1"), papel));
    }

    protected String token(Usuario usuario) {
        return "Bearer " + tokens.gerar(usuario);
    }

    protected String token(Papel papel) {
        return token(usuario(papel));
    }

    protected static UsuarioLogado logado(Usuario usuario) {
        return new UsuarioLogado(usuario.getId(), usuario.getEmail(), usuario.getPapel());
    }

    protected Recurso recurso(String nome, TipoRecurso tipo, Integer capacidade, String local, String competencia,
            boolean restrito, boolean manutencao) {
        Recurso recurso = new Recurso(nome, tipo);
        recurso.atualizar(nome, tipo, capacidade, local, competencia, restrito, manutencao);
        return recursos.save(recurso);
    }

    protected Recurso sala(String nome) {
        return recurso(nome, TipoRecurso.SALA, 30, "Bloco A", null, false, false);
    }

    protected Recurso salaRestrita(String nome) {
        return recurso(nome, TipoRecurso.SALA, 20, "Bloco B", null, true, false);
    }

    protected Recurso professor(String nome) {
        return recurso(nome, TipoRecurso.PROFESSOR, null, null, "Java", false, false);
    }

    protected Recurso material(String nome) {
        return recurso(nome, TipoRecurso.MATERIAL, null, "Almoxarifado", null, false, false);
    }

    protected ResultActions enviar(String url, String token, Object corpo) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo)));
    }

    protected ResultActions acao(String url, String token) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, token));
    }

    protected ResultActions consultar(String url, String token) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token));
    }

    protected ResultActions reservar(String token, LocalDateTime inicio, LocalDateTime fim, Recurso... recursosDaReserva)
            throws Exception {
        return enviar("/reservas", token, pedido(inicio, fim, recursosDaReserva));
    }

    protected ResultActions alterar(Long id, String token, LocalDateTime inicio, LocalDateTime fim,
            Recurso... recursosDaReserva) throws Exception {
        return mvc.perform(put("/reservas/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(pedido(inicio, fim, recursosDaReserva))));
    }

    /** Cria a reserva esperando 201 e devolve o id. */
    protected Long novaReserva(String token, LocalDateTime inicio, LocalDateTime fim, Recurso... recursosDaReserva)
            throws Exception {
        String corpo = reservar(token, inicio, fim, recursosDaReserva)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return lerJson(corpo).get("id").asLong();
    }

    protected JsonNode lerJson(String corpo) throws Exception {
        return json.readTree(corpo);
    }

    private static Map<String, Object> pedido(LocalDateTime inicio, LocalDateTime fim, Recurso... recursosDaReserva) {
        List<Long> ids = Arrays.stream(recursosDaReserva).map(Recurso::getId).toList();
        return Map.of("recursoIds", ids, "inicio", inicio.toString(), "fim", fim.toString(), "finalidade", "Aula");
    }
}
