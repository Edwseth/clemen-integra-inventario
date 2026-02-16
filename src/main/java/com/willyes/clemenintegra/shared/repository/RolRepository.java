package com.willyes.clemenintegra.shared.repository;

import com.willyes.clemenintegra.shared.model.rbac.RolEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByCodigoAndActivoTrue(String codigo);

    List<RolEntity> findByActivoTrueOrderByCodigoAsc();

    List<RolEntity> findAllByOrderByCodigoAsc();

    @Query(value = """
            select distinct r.codigo
            from usuarios_roles ur
            join roles r on r.id = ur.rol_id
            where ur.usuario_id = :usuarioId
              and r.codigo is not null
            """, nativeQuery = true)
    List<String> findCodigosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
