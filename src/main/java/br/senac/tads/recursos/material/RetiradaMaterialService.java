package br.senac.tads.recursos.material;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senac.tads.recursos.comum.ErroApi;
import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.recurso.TipoRecurso;
import br.senac.tads.recursos.reserva.Reserva;
import br.senac.tads.recursos.reserva.ReservaRepository;
import br.senac.tads.recursos.reserva.StatusReserva;
import br.senac.tads.recursos.seguranca.UsuarioLogado;

@Service
public class RetiradaMaterialService {

    public record PedidoMaterial(@NotNull Long materialId) {
    }

    public record RetiradaResposta(Long id, Long materialId, String material, LocalDateTime retiradaEm,
            String retiradoPor, LocalDateTime devolvidaEm, String recebidoPor) {
        static RetiradaResposta de(RetiradaMaterial r) {
            return new RetiradaResposta(r.getId(), r.getMaterial().getId(), r.getMaterial().getNome(),
                    r.getRetiradaEm(), r.getRetiradoPor(), r.getDevolvidaEm(), r.getRecebidoPor());
        }
    }

    private final RetiradaMaterialRepository retiradas;
    private final ReservaRepository reservas;

    public RetiradaMaterialService(RetiradaMaterialRepository retiradas, ReservaRepository reservas) {
        this.retiradas = retiradas;
        this.reservas = reservas;
    }

    @Transactional
    public RetiradaResposta retirar(Long reservaId, PedidoMaterial pedido, UsuarioLogado usuario) {
        Reserva reserva = carregar(reservaId);
        if (reserva.getStatus() != StatusReserva.APROVADA && reserva.getStatus() != StatusReserva.EM_USO) {
            throw ErroApi.conflito("Material só pode ser retirado de reserva aprovada ou em uso");
        }
        Recurso material = materialDaReserva(reserva, pedido.materialId());
        if (retiradas.findByReserva_IdAndMaterial_IdAndDevolvidaEmIsNull(reservaId, material.getId()).isPresent()) {
            throw ErroApi.conflito(material.getNome() + " já foi retirado e ainda não voltou");
        }
        return RetiradaResposta.de(retiradas.save(new RetiradaMaterial(reserva, material, usuario.email())));
    }

    @Transactional
    public RetiradaResposta devolver(Long reservaId, PedidoMaterial pedido, UsuarioLogado usuario) {
        Reserva reserva = carregar(reservaId);
        Recurso material = materialDaReserva(reserva, pedido.materialId());
        RetiradaMaterial retirada = retiradas
                .findByReserva_IdAndMaterial_IdAndDevolvidaEmIsNull(reservaId, material.getId())
                .orElseThrow(() -> ErroApi.conflito(material.getNome() + " não tem retirada em aberto"));
        retirada.devolver(usuario.email());
        return RetiradaResposta.de(retirada);
    }

    @Transactional(readOnly = true)
    public List<RetiradaResposta> listar(Long reservaId) {
        carregar(reservaId);
        return retiradas.findByReserva_IdOrderByIdAsc(reservaId).stream().map(RetiradaResposta::de).toList();
    }

    private Reserva carregar(Long id) {
        return reservas.findById(id).orElseThrow(() -> ErroApi.naoEncontrado("Reserva " + id + " não encontrada"));
    }

    private static Recurso materialDaReserva(Reserva reserva, Long materialId) {
        return reserva.getRecursos().stream()
                .filter(r -> r.getId().equals(materialId) && r.getTipo() == TipoRecurso.MATERIAL)
                .findFirst()
                .orElseThrow(() -> ErroApi.regraViolada("Material " + materialId + " não faz parte desta reserva"));
    }
}
