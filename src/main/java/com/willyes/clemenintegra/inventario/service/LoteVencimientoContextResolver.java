package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.config.InventoryVencidosProperties;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import com.willyes.clemenintegra.shared.model.Usuario;
import com.willyes.clemenintegra.shared.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoteVencimientoContextResolver {

    private final AlmacenRepository almacenRepository;
    private final UsuarioService usuarioService;

    public Long resolveAlmacenDestinoId(InventoryVencidosProperties properties) {
        Long configuredId = properties.getAlmacenDestinoId();
        if (configuredId != null) {
            if (!almacenRepository.existsById(configuredId)) {
                throw new IllegalStateException("No existe el almacén destino configurado para vencidos (id=" + configuredId + ")");
            }
            return configuredId;
        }

        String nombre = properties.requireAlmacenDestinoNombre();
        Almacen almacen = almacenRepository.findByNombreIgnoreCase(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el almacén destino de vencidos con nombre='" + nombre + "'"));
        if (almacen.getId() == null) {
            throw new IllegalStateException("El almacén destino de vencidos no tiene ID válido");
        }
        return almacen.getId().longValue();
    }

    public Usuario resolveUsuarioSistema() {
        return usuarioService.obtenerUsuarioSistemaJobVencimientos();
    }
}
