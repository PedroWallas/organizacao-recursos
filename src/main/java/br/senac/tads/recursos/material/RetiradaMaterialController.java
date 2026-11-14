package br.senac.tads.recursos.material;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senac.tads.recursos.material.RetiradaMaterialService.PedidoMaterial;
import br.senac.tads.recursos.material.RetiradaMaterialService.RetiradaResposta;
import br.senac.tads.recursos.seguranca.UsuarioLogado;

@RestController
@RequestMapping("/reservas/{reservaId}")
public class RetiradaMaterialController {

    private final RetiradaMaterialService service;

    public RetiradaMaterialController(RetiradaMaterialService service) {
        this.service = service;
    }

    @PostMapping("/retiradas")
    @ResponseStatus(HttpStatus.CREATED)
    public RetiradaResposta retirar(@PathVariable Long reservaId, @Valid @RequestBody PedidoMaterial pedido,
            @AuthenticationPrincipal Jwt token) {
        return service.retirar(reservaId, pedido, UsuarioLogado.de(token));
    }

    @PostMapping("/devolucoes")
    public RetiradaResposta devolver(@PathVariable Long reservaId, @Valid @RequestBody PedidoMaterial pedido,
            @AuthenticationPrincipal Jwt token) {
        return service.devolver(reservaId, pedido, UsuarioLogado.de(token));
    }

    @GetMapping("/retiradas")
    public List<RetiradaResposta> listar(@PathVariable Long reservaId) {
        return service.listar(reservaId);
    }
}
