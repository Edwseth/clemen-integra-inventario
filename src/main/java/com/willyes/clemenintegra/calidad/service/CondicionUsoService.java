package com.willyes.clemenintegra.calidad.service;

import com.willyes.clemenintegra.calidad.dto.CondicionUsoCreateDTO;
import com.willyes.clemenintegra.calidad.dto.CondicionUsoResponseDTO;
import com.willyes.clemenintegra.calidad.model.CondicionUso;

import java.util.List;

public interface CondicionUsoService {

    CondicionUso create(CondicionUsoCreateDTO dto, com.willyes.clemenintegra.shared.model.Usuario usuarioAutenticado);

    CondicionUso levantar(Long condicionId, com.willyes.clemenintegra.shared.model.Usuario usuarioAutenticado);

    List<CondicionUsoResponseDTO> getActivasByLote(Long loteId);
}
