package br.senac.tads.recursos.recurso;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.senac.tads.recursos.recurso.RecursoService.Filtro;
import br.senac.tads.recursos.recurso.RecursoService.RecursoDados;
import br.senac.tads.recursos.recurso.RecursoService.RecursoResposta;

@RestController
@RequestMapping("/recursos")
public class RecursoController {

    private final RecursoService service;

    public RecursoController(RecursoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecursoResposta> pesquisar(
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) Integer capacidadeMinima,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime livreDe,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime livreAte) {
        return service.pesquisar(new Filtro(tipo, capacidadeMinima, localizacao, competencia, livreDe, livreAte));
    }

    @GetMapping("/{id}")
    public RecursoResposta buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoResposta criar(@Valid @RequestBody RecursoDados dados) {
        return service.criar(dados);
    }

    @PutMapping("/{id}")
    public RecursoResposta atualizar(@PathVariable Long id, @Valid @RequestBody RecursoDados dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
