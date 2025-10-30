package com.willyes.clemenintegra.inventario.proveedor;

import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=none",
        "spring.jpa.defer-datasource-initialization=true",
        "DB_SECURPASS=dummy",
        "DB_SECURNAME=dummy"
})
@Sql(scripts = "classpath:sql/proveedor-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ProveedorRepositoryTest {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Test
    @DisplayName("findByProveedorContainingIgnoreCase aplica paginación y ordena por nombre")
    void findByProveedorContainingIgnoreCase_deberiaRespetarPaginacionYOrden() {
        proveedorRepository.save(Proveedor.builder()
                .nombre("Colombia Insumos")
                .identificacion("900111111")
                .telefono("1111111")
                .ciudad("Bogotá")
                .email("contacto1@demo.com")
                .direccion("Calle 1")
                .paginaWeb(null)
                .nombreContacto("Ana Perez")
                .activo(true)
                .build());

        proveedorRepository.save(Proveedor.builder()
                .nombre("Andes Componentes")
                .identificacion("900222222")
                .telefono("2222222")
                .ciudad("Medellín")
                .email("contacto2@demo.com")
                .direccion("Calle 2")
                .paginaWeb(null)
                .nombreContacto("Luis Gomez")
                .activo(true)
                .build());

        proveedorRepository.save(Proveedor.builder()
                .nombre("Colorantes Andinos")
                .identificacion("900333333")
                .telefono("3333333")
                .ciudad("Cali")
                .email("contacto3@demo.com")
                .direccion("Calle 3")
                .paginaWeb(null)
                .nombreContacto("Maria Ruiz")
                .activo(true)
                .build());

        Pageable pageable = PageRequest.of(0, 2, Sort.by("nombre").ascending());

        Page<Proveedor> page = proveedorRepository.findByProveedorContainingIgnoreCase("co", pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().get(0).getNombre()).isEqualTo("Andes Componentes");
        assertThat(page.getContent().get(1).getNombre()).isEqualTo("Colombia Insumos");
    }
}
