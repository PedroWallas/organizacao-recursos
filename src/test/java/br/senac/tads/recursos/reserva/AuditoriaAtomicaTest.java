package br.senac.tads.recursos.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.reserva.ReservaService.PedidoReserva;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.suporte.TesteIntegracao;

/** Se o historico nao puder ser gravado, a reserva tambem nao pode ficar: auditoria e mudanca andam juntas. */
class AuditoriaAtomicaTest extends TesteIntegracao {

    @MockitoBean
    private HistoricoReservaRepository historico;

    @Autowired
    private ReservaService service;
    @Autowired
    private ReservaRepository reservas;

    @Test
    void falhaAoGravarHistoricoDesfazAReserva() {
        when(historico.save(any())).thenThrow(new DataAccessResourceFailureException("disco cheio"));
        Recurso sala = sala("Sala 1");
        PedidoReserva pedido = new PedidoReserva(List.of(sala.getId()), AMANHA_8H, AMANHA_8H.plusHours(1), null);
        var ana = logado(usuario(Papel.SOLICITANTE));

        assertThatThrownBy(() -> service.criar(pedido, ana)).isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(reservas.count()).isZero();
    }
}
