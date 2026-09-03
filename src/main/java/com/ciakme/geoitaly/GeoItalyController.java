package com.ciakme.geoitaly;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoItalyController {

    //Se non inserisci final, non viene inizializzato dalla notazione lombook:@RequiredArgsConstructor
    private final GeoItalyService geoItalyService;

    @GetMapping("/comuni/search")
    public List<GeoItalyResponse> search(@RequestParam("q") String nome) {
        return geoItalyService.searchComune(nome);
    }
}
