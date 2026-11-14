package br.senac.tads.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import br.senac.tads.recursos.comum.ErroApi;
import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.reserva.ReservaService.PedidoReserva;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.seguranca.UsuarioLogado;
import br.senac.tads.recursos.suporte.TesteIntegracao;

/** Pedidos disparados ao mesmo tempo para o mesmo horario: so um pode ganhar. */
class ConcorrenciaTest extends TesteIntegracao {

    private static final int PEDIDOS = 10;

    @Autowired
    private ReservaService service;
    @Autowired
    private ReservaRepository reservas;
    @Autowired
    private ConflitoEvitadoRepository conflitos;

    @Test
    void dezPedidosSimultaneosParaAMesmaSalaGeramUmaReserva() throws Exception {
        Recurso sala = sala("Sala 1");

        List<HttpStatus> resultados = disparar(i -> pedido(sala));

        assertThat(resultados).filteredOn(HttpStatus.CREATED::equals).hasSize(1);
        assertThat(resultados).filteredOn(HttpStatus.CONFLICT::equals).hasSize(PEDIDOS - 1);
        assertThat(reservas.count()).isEqualTo(1);
        assertThat(conflitos.count()).isEqualTo(PEDIDOS - 1);
    }

    @Test
    void mesmoProfessorEmSalasDiferentesAoMesmoTempoGeraUmaReserva() throws Exception {
        Recurso professor = professor("Prof. Lia");
        List<Recurso> salas = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            salas.add(sala("Sala " + i));
        }

        List<HttpStatus> resultados = disparar(i -> pedido(salas.get(i), professor));

        assertThat(resultados).filteredOn(HttpStatus.CREATED::equals).hasSize(1);
        assertThat(reservas.count()).isEqualTo(1);
    }

    private static PedidoReserva pedido(Recurso... recursos) {
        List<Long> ids = Arrays.stream(recursos).map(Recurso::getId).toList();
        return new PedidoReserva(ids, AMANHA_8H, AMANHA_8H.plusHours(2), "Aula");
    }

    private List<HttpStatus> disparar(IntFunction<PedidoReserva> pedidoDoUsuario) throws Exception {
        List<UsuarioLogado> pessoas = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            pessoas.add(logado(usuario(Papel.SOLICITANTE)));
        }
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(PEDIDOS);
        try {
            List<Future<HttpStatus>> futuros = new ArrayList<>();
            for (int i = 0; i < PEDIDOS; i++) {
                PedidoReserva pedido = pedidoDoUsuario.apply(i);
                UsuarioLogado pessoa = pessoas.get(i);
                Callable<HttpStatus> tarefa = () -> {
                    largada.await();
                    try {
                        service.criar(pedido, pessoa);
                        return HttpStatus.CREATED;
                    } catch (ErroApi e) {
                        return e.getStatus();
                    }
                };
                futuros.add(executor.submit(tarefa));
            }
            largada.countDown();
            List<HttpStatus> resultados = new ArrayList<>();
            for (Future<HttpStatus> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }
}
