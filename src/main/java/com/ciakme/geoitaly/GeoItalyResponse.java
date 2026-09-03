package com.ciakme.geoitaly;


import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class GeoItalyResponse {

    private Integer id;
    private String nomeComune;
    private String siglaProvincia;
    private BigDecimal latitudine;
    private BigDecimal longitudine;


    public static GeoItalyResponse from(Comune comune){
        return GeoItalyResponse.builder()
                .id(comune.getId())
                .nomeComune(comune.getNome())
                .siglaProvincia(comune.getProvincia().getSigla())
                .latitudine(comune.getLatitudine())
                .longitudine(comune.getLongitudine())
                .build();
    }

    public Integer getId() {
        return id;
    }

    public String getNomeComune() {
        return nomeComune;
    }

    public String getSiglaProvincia() {
        return siglaProvincia;
    }

    public BigDecimal getLatitudine() {
        return latitudine;
    }

    public BigDecimal getLongitudine() {
        return longitudine;
    }
}
