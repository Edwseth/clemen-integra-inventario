package com.willyes.clemenintegra.shared.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RolPermisoRepository {

    private final JdbcTemplate jdbcTemplate;

    public RolPermisoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteByRolId(Long rolId) {
        jdbcTemplate.update("delete from roles_permisos where rol_id = ?", rolId);
    }

    public void insertBatch(Long rolId, List<Long> permisoIds) {
        jdbcTemplate.batchUpdate(
                "insert into roles_permisos (rol_id, permiso_id) values (?, ?)",
                permisoIds,
                permisoIds.size(),
                (ps, permisoId) -> {
                    ps.setLong(1, rolId);
                    ps.setLong(2, permisoId);
                }
        );
    }
}
