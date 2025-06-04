package com.willyes.clemenintegra.inventario.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorRequestDTO {

    @NotBlank
    private String nombre;

    @NotBlank
    private String identificacion;

    private String telefono;

    @Email
    private String email;

    private String direccion;

    private String paginaWeb;

    @NotBlank
    private String nombreContacto;

    @NotNull
    private Boolean activo;
}

