package com.ciakme.domain.user;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/*

* @Service — dice a Spring che questa classe è un componente di servizio.
* Spring la registra come bean e la rende disponibile per l'injection in altri componenti.

* @RequiredArgsConstructor
* un'altra annotazione Lombok. Genera un costruttore con tutti i campi final come parametri.
* Spring usa questo costruttore per iniettare automaticamente le dipendenze (UserRepository e PasswordEncoder).
* È il modo moderno di fare Dependency Injection in Spring — molto meglio di @Autowired sul campo che trovi nei tutorial vecchi.

* Perché final? Perché le dipendenze non devono mai cambiare dopo l'inizializzazione.
* Se un campo è final e @RequiredArgsConstructor genera il costruttore, Spring capisce da solo cosa iniettare.
*

* @Transactional —
* dice a Spring di eseguire il metodo in una transazione database.
* Se qualcosa va storto a metà metodo (es. errore durante il save),
* Spring fa automaticamente il rollback — annulla tutto come se non fosse successo niente.
* Fondamentale per mantenere il database in uno stato consistente.
*

* Nota che findByEmail e findById non hanno @Transactional —
* sono operazioni di sola lettura, non modificano dati, quindi non serve la transazione.
*

* passwordEncoder.encode(rawPassword) — non salviamo mai la password in chiaro nel database.
* Il PasswordEncoder (che configureremo in SecurityConfig) usa BCrypt per generare un hash sicuro.
* BCrypt aggiunge automaticamente un "salt" casuale, quindi due hash della stessa password sono sempre diversi.
* */


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    // Crea un nuovo utente — usato durante la registrazione
    @Transactional
    public User CreateUser(String email, String rawPassword, UserRole userRole){

        // Verifica che l'email non sia già registrata
        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("Email già registrata: " +  email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(userRole)
                .locale("it")
                .isVerified(false)
                .build();

        return userRepository.save(user);
    }

    // Cerca un utente per email
    public Optional<User> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    // Cerca un utente per ID
    public Optional<User> findByID(UUID id){
        return userRepository.findById(id);
    }

    // Segna l'utente come verificato
    @Transactional
    public void verifyUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException(
                "Utente non trovato: " + userId
        ));
        user.setVerified(true);
        userRepository.save(user);
    }

}
