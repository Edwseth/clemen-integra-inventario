package com.willyes.clemenintegra.produccion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lote_consecutivo_dia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteConsecutivoDia {

    @Id
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "ultimo_valor", nullable = false)
    private int ultimoValor;
}
