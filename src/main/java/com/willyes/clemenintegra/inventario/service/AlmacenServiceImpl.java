package com.willyes.clemenintegra.inventario.service;

import com.willyes.clemenintegra.inventario.dto.AlmacenResponseDTO;
import com.willyes.clemenintegra.inventario.model.Almacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoAlmacen;
import com.willyes.clemenintegra.inventario.model.enums.TipoCategoria;
import com.willyes.clemenintegra.inventario.repository.AlmacenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlmacenServiceImpl implements AlmacenService {

    private final AlmacenRepository repository;

    @Override
    public Page<AlmacenResponseDTO> buscarPorTipoYCategoria(TipoAlmacen tipo, TipoCategoria categoria, Pageable pageable) {
        Page<Almacen> page;
        if (tipo != null && categoria != null) {
            page = repository.findByTipoAndCategoria(tipo, categoria, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(AlmacenResponseDTO::new);
    }
}
