package com.ciakme.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

/*
*
*
@Configuration — dice a Spring che questa classe contiene definizioni di bean.
Spring la scansiona all'avvio e registra tutti i metodi annotati con @Bean.

@Bean — ogni metodo annotato con @Bean produce un oggetto che Spring gestisce nel suo Application Context —
il contenitore di tutti i bean dell'applicazione. Quando UserService dichiara private final PasswordEncoder passwordEncoder, Spring cerca nel suo context un bean di tipo PasswordEncoder e lo inietta automaticamente — trova esattamente quello che abbiamo definito qui.

BCryptPasswordEncoder — BCrypt è l'algoritmo di hashing delle password più usato in ambito enterprise. Ha tre caratteristiche fondamentali:

Aggiunge automaticamente un salt casuale — due hash della stessa password sono sempre diversi
È lento per design — ogni hash richiede decine di millisecondi, rendendo gli attacchi brute-force praticamente impossibili
Ha un cost factor configurabile — puoi renderlo più lento man mano che i computer diventano più veloci

SecurityFilterChain — definisce le regole di sicurezza HTTP. Per ora abbiamo una configurazione aperta che permette tutto — è temporanea.
Quando implementeremo JWT, torneremo qui e aggiungeremo:

Il filtro che legge il token JWT da ogni richiesta
Le regole su quali endpoint richiedono autenticazione e quali no
I ruoli necessari per ogni endpoint

SessionCreationPolicy.STATELESS — dice a Spring Security di non creare mai sessioni HTTP.
In un'API REST moderna ogni richiesta deve essere autonoma e portare con sé le credenziali (il token JWT) — non ci sono cookie di sessione.
Questo è fondamentale per scalabilità: con le sessioni il server deve ricordare lo stato di ogni utente connesso,
con JWT stateless qualsiasi istanza del server può rispondere a qualsiasi richiesta.

CSRF disabilitato — CSRF (Cross-Site Request Forgery) è un attacco che sfrutta i cookie di sessione.
Siccome non usiamo sessioni né cookie ma JWT, CSRF non è applicabile — disabilitarlo elimina complessità inutile.
*
* */


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Registra BCrypt come implementazione di PasswordEncoder
    // Questo è il bean che UserService usa per cifrare le password
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Configurazione base della sicurezza HTTP
    // Per ora disabilitiamo tutto — lo configureremo con JWT dopo
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
                // Disabilita CSRF — non serve per API REST stateless
                .csrf(csrf -> csrf.disable())
                // Nessuna sessione HTTP — useremo JWT stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Per ora permettiamo tutto — aggiungeremo i vincoli con JWT
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}