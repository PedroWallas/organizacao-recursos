package br.senac.tads.recursos.recurso;

import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {

    /**
     * Trava as linhas dos recursos ate o fim da transacao. Duas reservas simultaneas
     * para o mesmo recurso ficam em fila aqui. A ordem por id evita deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recurso r where r.id in :ids order by r.id")
    List<Recurso> travar(Collection<Long> ids);

    List<Recurso> findAllByOrderByNomeAsc();
}
