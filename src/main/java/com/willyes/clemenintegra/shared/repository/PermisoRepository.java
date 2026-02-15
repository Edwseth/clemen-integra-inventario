package com.willyes.clemenintegra.shared.repository;

import com.willyes.clemenintegra.shared.model.rbac.PermisoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermisoRepository extends JpaRepository<PermisoEntity, Long> {

    List<PermisoEntity> findByRolesCodigoAndActivoTrue(String codigo);

    List<PermisoEntity> findByModuloIgnoreCaseOrderByCodigoAsc(String modulo);

    List<PermisoEntity> findByModuloIgnoreCaseAndActivoOrderByCodigoAsc(String modulo, Boolean activo);

    List<PermisoEntity> findByRolesIdOrderByCodigoAsc(Long rolId);

    @Query(value = """
            select distinct p.codigo
            from usuarios_roles ur
            join roles r on r.id = ur.rol_id
            join roles_permisos rp on rp.rol_id = r.id
            join permisos p on p.id = rp.permiso_id
            where ur.usuario_id = :usuarioId
              and r.activo = true
              and p.activo = true
              and p.codigo is not null
            """, nativeQuery = true)
    List<String> findCodigosPermisosActivosByUsuarioId(@Param("usuarioId") Long usuarioId);
}
