package com.willyes.clemenintegra.shared.repository;

import com.willyes.clemenintegra.shared.model.rbac.RolEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByCodigoAndActivoTrue(String codigo);
}
