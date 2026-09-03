package com.ciakme.geoitaly;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeoItalyService {

    private final List<Comune> comuni;

    public GeoItalyService(GeoItalyRepository geoItalyRepository){
        this.comuni = geoItalyRepository.findAllWithProvincia();
    }

    public List<GeoItalyResponse> searchComune(String nome) {
        if(nome == null || nome.isBlank()){
            return List.of();
        }

        String normalizedString = nome.toLowerCase();

        return comuni.stream()
                .filter(c -> c.getNome().toLowerCase().startsWith(normalizedString))
                .limit(5)
                .map(GeoItalyResponse::from)
                .toList();

    }
}
