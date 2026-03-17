package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoteVencimientoContextResolverTest {

    @Mock
    private AlmacenRepository almacenRepository;
    @Mock
    private UsuarioService usuarioService;

    @Test
    void resolveAlmacenDestinoId_buscaPorNombreObsoletosCuandoNoHayId() {
        InventoryVencidosProperties properties = new InventoryVencidosProperties();
        properties.setAlmacenDestinoId(null);
        properties.setAlmacenDestinoNombre("Obsoletos");

        Almacen almacen = new Almacen(9);
        when(almacenRepository.findByNombreIgnoreCase("Obsoletos")).thenReturn(Optional.of(almacen));

        LoteVencimientoContextResolver resolver = new LoteVencimientoContextResolver(almacenRepository, usuarioService);
        Long id = resolver.resolveAlmacenDestinoId(properties);

        assertThat(id).isEqualTo(9L);
    }
}
