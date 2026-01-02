-- Ajusta el tipo de dato para alinearlo con productos.id y evitar error 3780 en la FK
ALTER TABLE conteos_ciclicos_detalle
    MODIFY producto_id INT NOT NULL;
