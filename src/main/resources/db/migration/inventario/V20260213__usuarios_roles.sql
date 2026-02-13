-- V__usuarios_roles.sql
-- Crea relación usuarios ↔ roles sin eliminar usuarios.rol (ENUM).
-- Pobla desde usuarios.rol -> roles.codigo (códigos ya confirmados como compatibles).

CREATE TABLE IF NOT EXISTS usuarios_roles (
  usuario_id BIGINT NOT NULL,
  rol_id BIGINT NOT NULL,
  PRIMARY KEY (usuario_id, rol_id),
  CONSTRAINT fk_usuarios_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  CONSTRAINT fk_usuarios_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
);

-- Poblamiento idempotente: no duplica, no rompe si se re-ejecuta en entornos que ya tienen datos.
INSERT INTO usuarios_roles (usuario_id, rol_id)
SELECT u.id, r.id
FROM usuarios u
JOIN roles r ON r.codigo = u.rol
WHERE u.rol IS NOT NULL
ON DUPLICATE KEY UPDATE rol_id = rol_id;