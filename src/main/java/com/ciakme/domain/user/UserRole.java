package com.ciakme.domain.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

//Tre valori che corrispondono esattamente ai tre tipi di utente che abbiamo definito.
public enum UserRole {
    TALENT,
    AGENCY,
    CAST_AGENCY;



    @JsonCreator
    public static UserRole fromString(String role){
        if(role == null) return null;
        for(UserRole u : UserRole.values()){
            if(u.name().equalsIgnoreCase(role)){
                return u;
            }
        }
        throw new IllegalArgumentException(
                "Ruolo non valido: " + role + ". Ammessi solo valori: TALENT, AGENCY, CAST_AGENCY"
        );
    }

    @JsonValue
    public String toJson(){
        return this.name();
    }
}
