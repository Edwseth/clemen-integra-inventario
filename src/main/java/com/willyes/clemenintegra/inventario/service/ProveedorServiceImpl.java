package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.ProveedorResponseDTO;
import com.willyes.clemenintegra.inventario.mapper.ProveedorMapper;
import com.willyes.clemenintegra.inventario.model.Proveedor;
import com.willyes.clemenintegra.inventario.proveedor.dto.ProveedorAutocompleteDTO;
import com.willyes.clemenintegra.inventario.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository repository;
    private final ProveedorMapper mapper;

    @Override
    public Page<ProveedorResponseDTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDTO);
    }

    @Override
    public Page<ProveedorAutocompleteDTO> buscarAutocomplete(String term, Pageable pageable) {
        return repository.findByProveedorContainingIgnoreCase(term, pageable)
                .map(this::mapToAutocompleteDto);
    }

    private ProveedorAutocompleteDTO mapToAutocompleteDto(Proveedor proveedor) {
        Long id = proveedor.getId() != null ? proveedor.getId().longValue() : null;
        return ProveedorAutocompleteDTO.builder()
                .id(id)
                .label(proveedor.getNombre())
                .nit(proveedor.getIdentificacion())
                .ciudad(proveedor.getCiudad())
                .build();
    }
}
