package br.senac.tads.recursos.reserva;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senac.tads.recursos.reserva.ReservaService.HistoricoResposta;
import br.senac.tads.recursos.reserva.ReservaService.PedidoReserva;
import br.senac.tads.recursos.reserva.ReservaService.ReservaResposta;
import br.senac.tads.recursos.seguranca.UsuarioLogado;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResposta criar(@Valid @RequestBody PedidoReserva pedido, @AuthenticationPrincipal Jwt token) {
        return service.criar(pedido, UsuarioLogado.de(token));
    }

    @GetMapping
    public List<ReservaResposta> listar(@AuthenticationPrincipal Jwt token) {
        return service.listar(UsuarioLogado.de(token));
    }

    @GetMapping("/{id}")
    public ReservaResposta buscar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.buscar(id, UsuarioLogado.de(token));
    }

    @PutMapping("/{id}")
    public ReservaResposta alterar(@PathVariable Long id, @Valid @RequestBody PedidoReserva pedido,
            @AuthenticationPrincipal Jwt token) {
        return service.alterar(id, pedido, UsuarioLogado.de(token));
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoResposta> historico(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.historico(id, UsuarioLogado.de(token));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResposta cancelar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.cancelar(id, UsuarioLogado.de(token));
    }

    @PostMapping("/{id}/aprovar")
    public ReservaResposta aprovar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.aprovar(id, UsuarioLogado.de(token));
    }

    @PostMapping("/{id}/rejeitar")
    public ReservaResposta rejeitar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.rejeitar(id, UsuarioLogado.de(token));
    }

    @PostMapping("/{id}/iniciar")
    public ReservaResposta iniciar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.iniciar(id, UsuarioLogado.de(token));
    }

    @PostMapping("/{id}/concluir")
    public ReservaResposta concluir(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.concluir(id, UsuarioLogado.de(token));
    }

    @PostMapping("/{id}/nao-compareceu")
    public ReservaResposta naoCompareceu(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return service.registrarAusencia(id, UsuarioLogado.de(token));
    }
}
