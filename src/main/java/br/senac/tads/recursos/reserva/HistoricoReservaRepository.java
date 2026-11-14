package br.senac.tads.recursos.reserva;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoReservaRepository extends JpaRepository<HistoricoReserva, Long> {

    List<HistoricoReserva> findByReserva_IdOrderByIdAsc(Long reservaId);
}
