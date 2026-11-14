package br.senac.tads.recursos.reserva;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConflitoEvitadoRepository extends JpaRepository<ConflitoEvitado, Long> {

    interface TotalPorRecurso {
        Long getRecursoId();

        Long getTotal();
    }

    @Query("select c.recursoId as recursoId, count(c) as total from ConflitoEvitado c group by c.recursoId")
    List<TotalPorRecurso> totalPorRecurso();
}
