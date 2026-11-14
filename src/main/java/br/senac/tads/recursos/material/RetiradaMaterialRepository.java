package br.senac.tads.recursos.material;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RetiradaMaterialRepository extends JpaRepository<RetiradaMaterial, Long> {

    boolean existsByReserva_IdAndDevolvidaEmIsNull(Long reservaId);

    Optional<RetiradaMaterial> findByReserva_IdAndMaterial_IdAndDevolvidaEmIsNull(Long reservaId, Long materialId);

    List<RetiradaMaterial> findByReserva_IdOrderByIdAsc(Long reservaId);
}
