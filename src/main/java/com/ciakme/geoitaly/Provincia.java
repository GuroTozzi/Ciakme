package com.ciakme.geoitaly;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "province")
public class Provincia {

    @Id
    private Integer id;
    private String nome;
    private String sigla;


    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSigla() {
        return sigla;
    }
}
