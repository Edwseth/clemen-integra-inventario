package com.willyes.clemenintegra.shared.repository;

import com.willyes.clemenintegra.shared.model.rbac.RolEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByCodigoAndActivoTrue(String codigo);

    List<RolEntity> findByActivoTrueOrderByCodigoAsc();

    List<RolEntity> findAllByOrderByCodigoAsc();
}
