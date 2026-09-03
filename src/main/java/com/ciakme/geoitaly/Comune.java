package com.ciakme.geoitaly;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "Comuni")
@Getter
@NoArgsConstructor
public class Comune {

    @Id
    private Integer id;

    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id", nullable = false)
    private Provincia provincia;

    @Column(name = "capoluogo_provincia")
    private Boolean capoluogoProvincia;

    @Column(name = "codice_catastale")
    private String codiceCatastale;

    @Column(name = "latitudine")
    private BigDecimal latitudine;

    @Column(name = "longitudine")
    private BigDecimal  longitudine;

}
