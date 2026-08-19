package com.ciakme.domain.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;


    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = LocalDateTime.now();
    }
}

/*
* Spiegazione delle annotazioni — queste le troverai ovunque

@Entity — dice a JPA/Hibernate che questa classe è un'entità, ovvero corrisponde a una tabella nel database.

@Table(name = "users") — specifica il nome esatto della tabella. Senza questa annotazione Hibernate cercherebbe una tabella chiamata user (il nome della classe), ma la nostra si chiama users.

@Id — indica la chiave primaria.

@GeneratedValue(strategy = GenerationType.UUID) — dice a Hibernate di generare automaticamente un UUID per ogni nuovo record, equivalente al DEFAULT gen_random_uuid() che abbiamo messo nel SQL.

@Column — mappa il campo Java alla colonna del database. nullable = false corrisponde al NOT NULL nel SQL, unique = true corrisponde a UNIQUE, length alla dimensione del VARCHAR.

@Enumerated(EnumType.STRING) — dice a Hibernate di salvare l'enum come stringa ("TALENT", "AGENCY", "CAST_AGENCY") invece che come numero intero. Importante — senza questa annotazione salverebbe 0, 1, 2 e se un giorno riordini l'enum tutto si rompe.

@PrePersist — metodo che Spring chiama automaticamente prima di salvare l'entità nel database per la prima volta. Lo usiamo per impostare createdAt con il timestamp corrente — equivalente al DEFAULT NOW() nel SQL, ma gestito lato Java.

Lombok — le quattro annotazioni @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor generano automaticamente tutto il boilerplate Java:

@Data → getter, setter, equals(), hashCode(), toString()
@Builder → pattern builder per costruire oggetti in modo fluente: User.builder().email("...").role(TALENT).build()
@NoArgsConstructor → costruttore senza parametri (richiesto da JPA)
@AllArgsConstructor → costruttore con tutti i parametri (usato dal Builder)
*
*
*
* */