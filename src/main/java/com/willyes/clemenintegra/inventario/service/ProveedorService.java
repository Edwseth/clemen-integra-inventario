package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ProveedorResponseDTO;
import com.willyes.clemenintegra.inventario.proveedor.dto.ProveedorAutocompleteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProveedorService {
    Page<ProveedorResponseDTO> listar(Pageable pageable);
    Page<ProveedorAutocompleteDTO> buscarAutocomplete(String term, Pageable pageable);
}
