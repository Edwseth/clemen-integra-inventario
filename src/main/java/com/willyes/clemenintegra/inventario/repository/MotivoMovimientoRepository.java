package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.MotivoMovimiento;
import com.willyes.clemenintegra.inventario.model.enums.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MotivoMovimientoRepository extends JpaRepository<MotivoMovimiento, Long> {

    Optional<MotivoMovimiento> findByMotivo(TipoMovimiento motivo);

}
