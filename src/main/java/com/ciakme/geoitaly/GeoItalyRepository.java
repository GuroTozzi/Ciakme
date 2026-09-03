package com.ciakme.geoitaly;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface GeoItalyRepository extends JpaRepository<Comune, Integer> {

    @Query("SELECT c FROM Comune c JOIN FETCH c.provincia")
    ArrayList<Comune> findAllWithProvincia();
}
