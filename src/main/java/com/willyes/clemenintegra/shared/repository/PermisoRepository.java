package com.willyes.clemenintegra.shared.repository;

import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoRepository extends JpaRepository<PermisoEntity, Long> {

    List<PermisoEntity> findByRolesCodigoAndActivoTrue(String codigo);
}
