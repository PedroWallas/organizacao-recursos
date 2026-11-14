package br.senac.tads.recursos.reserva;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Id que nunca existe, usado quando nao ha reserva a ignorar na checagem. */
    long NENHUMA = 0L;

    /** Dois periodos se sobrepoem quando um comeca antes do outro terminar. Encostar (fim == inicio) nao conta. */
    @Query("""
            select count(r) > 0 from Reserva r join r.recursos rc
            where rc.id = :recursoId
              and r.status in :status
              and r.inicio < :fim
              and r.fim > :inicio
              and r.id <> :ignorar
            """)
    boolean existeReservaNoPeriodo(Long recursoId, LocalDateTime inicio, LocalDateTime fim, Long ignorar,
            Collection<StatusReserva> status);

    default boolean existeConflito(Long recursoId, LocalDateTime inicio, LocalDateTime fim, long ignorar) {
        return existeReservaNoPeriodo(recursoId, inicio, fim, ignorar, StatusReserva.OCUPAM_AGENDA);
    }

    @Query("""
            select distinct rc.id from Reserva r join r.recursos rc
            where r.status in :status
              and r.inicio < :fim
              and r.fim > :inicio
            """)
    Set<Long> idsComReservaNoPeriodo(LocalDateTime inicio, LocalDateTime fim, Collection<StatusReserva> status);

    /** Todos os recursos ocupados no periodo, numa consulta so (a pesquisa nao precisa ir recurso por recurso). */
    default Set<Long> recursosOcupados(LocalDateTime inicio, LocalDateTime fim) {
        return idsComReservaNoPeriodo(inicio, fim, StatusReserva.OCUPAM_AGENDA);
    }

    boolean existsByRecursos_Id(Long recursoId);

    List<Reserva> findAllByOrderByInicioAsc();

    List<Reserva> findBySolicitante_IdOrderByInicioAsc(Long solicitanteId);

    @Query("select distinct r from Reserva r join fetch r.recursos where r.status in :status")
    List<Reserva> buscarComRecursos(Collection<StatusReserva> status);
}
