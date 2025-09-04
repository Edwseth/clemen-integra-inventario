package com.willyes.clemenintegra.inventario.repository;

import com.willyes.clemenintegra.inventario.model.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    Optional<UnidadMedida> findByNombre(String nombre);

    /**
     * Busca una unidad de medida por su nombre o símbolo, ignorando mayúsculas/minúsculas.
     * Permite que los consumidores usen indistintamente el nombre completo ("Gramo")
     * o el símbolo ("g") al referirse a la unidad.
     */
    Optional<UnidadMedida> findByNombreIgnoreCaseOrSimboloIgnoreCase(String nombre, String simbolo);

}

