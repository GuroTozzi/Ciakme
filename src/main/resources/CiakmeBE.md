# CiakMe — Backend Java — Contesto per Claude Code

Leggi questo file per intero prima di scrivere qualsiasi codice.
È il contesto accumulato durante lo sviluppo del backend Java di CiakMe.

---

## Stack tecnico esatto

- **Java 21** — Eclipse Temurin 21.0.12
- **Spring Boot 4.1.0** con Spring 7.0.8
- **Hibernate ORM 7.4.1.Final**
- **PostgreSQL 18** con PostGIS installato (non ancora attivato nello schema)
- **Flyway 12.4.0** per le migrazioni
- **Spring Security 7.1.0** con JWT (jjwt 0.12.6)
- **Lombok 1.18.46**
- **Maven 3.9.9**
- **IDE**: IntelliJ IDEA 2026.1

---

## Struttura del progetto

```
com.ciakme/
├── domain/
│   ├── user/           ← entità User, enum UserRole, Repository, Service, Controller
│   ├── talent/         ← da implementare: TalentProfile, TalentMedia, AvailabilitySlot
│   ├── pro/            ← da implementare: ProAccount, PrivateDatabase, PrivateDbEntry
│   ├── casting/        ← da implementare: Project, Role, Application, RoleRequest
│   ├── visibility/     ← da implementare: TalentAgencyVisibility
│   └── gamification/   ← da implementare: Badge, TalentBadge, Review
├── api/                ← DTO condivisi (se servono cross-domain)
└── security/           ← SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter
```

Ogni package `domain/X` contiene:
- `X.java` — entità JPA
- `XRepository.java` — interfaccia Spring Data JPA
- `XService.java` — logica di business
- `XController.java` — endpoint REST
- `XRequest.java` / `XResponse.java` — DTO per input/output HTTP

---

## Stato attuale — cosa è già implementato

### `domain/user/`

**`UserRole.java`** — enum con `@JsonCreator` e `@JsonValue` per deserializzazione JSON:
```java
public enum UserRole { TALENT, AGENCY, CAST_AGENCY }
```

**`User.java`** — entità JPA mappata su tabella `users`:
- ID: `UUID` generato manualmente in `@PrePersist` con `UUID.randomUUID()`
  (NON usare `@GeneratedValue` — Hibernate 7 con UUID causa problemi con sequenze)
- Enum PostgreSQL nativi: richiedono `@JdbcType(PostgreSQLEnumJdbcType.class)`
  (senza questo, PostgreSQL rifiuta l'INSERT con "expression is of type character varying")
- `@PrePersist` imposta `id` e `createdAt`

**`UserRepository.java`** — estende `JpaRepository<User, UUID>`:
- `findByEmail(String email)` → `Optional<User>`
- `existsByEmail(String email)` → `boolean`

**`UserService.java`** — logica di registrazione e ricerca utenti

**`UserController.java`** — endpoint:
- `POST /api/users/register` → `201 Created` con `UserResponse`
- `GET /api/users/{id}` → `200 OK` o `404 Not Found`

**`RegisterRequest.java`** — DTO con validazione `@Email`, `@NotBlank`, `@NotNull`

**`UserResponse.java`** — DTO di risposta, NON espone `passwordHash`

### `security/`

**`SecurityConfig.java`**:
- `BCryptPasswordEncoder` registrato come bean
- Security configurata come STATELESS (JWT, no sessioni)
- CSRF disabilitato
- Per ora tutti gli endpoint sono `permitAll()` — da restringere con JWT

---

## Pattern obbligatori — leggi prima di scrivere entità

### 1. UUID — generazione manuale in @PrePersist

```java
// CORRETTO — Hibernate 7 con PostgreSQL
@Id
@JdbcTypeCode(SqlTypes.UUID)
@Column(name = "id", updatable = false, nullable = false)
private UUID id;

@PrePersist
protected void onCreate() {
    if (this.id == null) {
        this.id = UUID.randomUUID();
    }
    this.createdAt = LocalDateTime.now();
}

// SBAGLIATO — causa "missing sequence" con Hibernate 7
// @GeneratedValue(strategy = GenerationType.UUID)  ← NON usare
```

### 2. Enum PostgreSQL nativi — @JdbcType obbligatorio

```java
// CORRETTO — PostgreSQL ha colonne enum native (user_role, talent_type, ecc.)
@Enumerated(EnumType.STRING)
@JdbcType(PostgreSQLEnumJdbcType.class)
@Column(name = "role", nullable = false)
private UserRole role;

// SBAGLIATO — causa "expression is of type character varying"
// @Enumerated(EnumType.STRING) da solo non basta con PostgreSQL enum nativi
```

### 3. Enum Java — @JsonCreator per deserializzazione JSON

```java
@JsonCreator
public static UserRole fromString(String value) {
    if (value == null) return null;
    for (UserRole r : UserRole.values()) {
        if (r.name().equalsIgnoreCase(value)) return r;
    }
    throw new IllegalArgumentException("Valore non valido: " + value);
}

// NON usare valueOf() dentro @JsonCreator — causa loop infinito con Jackson
```

### 4. DTO separati dalle entità

- Mai esporre l'entità JPA direttamente nei Controller
- Ogni endpoint riceve un `XRequest` e restituisce un `XResponse`
- Il metodo statico `from(Entity e)` converte entità → DTO
- Mai esporre campi sensibili (passwordHash, ecc.) nei DTO di risposta

### 5. Dipendency Injection — costruttore, non @Autowired

```java
// CORRETTO — con @RequiredArgsConstructor di Lombok
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
}

// SBAGLIATO — @Autowired sul campo è deprecato e non testabile
// @Autowired
// private UserRepository userRepository;
```

---

## Schema database — tabelle e distribuzione futura microservizi

Lo schema completo è in `src/main/resources/db/migration/V1__init_schema.sql`.

In questa fase monolitica tutte le tabelle sono in un unico database `ciakme`.
In futuro (migrazione a microservizi) verranno distribuite così:

| Microservizio | Tabelle |
|---|---|
| Auth Service | `users` |
| Talent Service | `talent_profiles`, `talent_media`, `availability_slots` |
| Pro Service | `pro_accounts`, `private_databases`, `private_db_entries` |
| Casting Service | `projects`, `roles`, `applications`, `role_requests`, `role_request_proposals`, `script_analyses` |
| Visibility Service | `talent_agency_visibility` |
| Gamification Service | `badges`, `talent_badges`, `reviews` |
| Notification Service | `notifications`, `email_logs` |

**Importante**: le FK cross-domain (es. `talent_id` in `private_db_entries`)
non sono FK reali nel codice — l'integrità è garantita a livello applicativo.
Nel monolite attuale le FK SQL esistono; nei microservizi futuri spariranno.

---

## Modello di dominio — decisioni chiave

### Tipi di utente

```
TALENT → si registra come attore o comparsa
AGENCY → gestisce attori con contratti formali, ha 1 DB privato
CAST_AGENCY → gestisce casting comparse, ha 1 DB privato, contatta Agency per attori
```

### Tipi di talento

```
ACTOR → visibility_mode = RESTRICTED obbligatorio
         appartiene a 1+ Agency tramite contratto
         invisibile al pubblico e alle Cast Agency

EXTRA → visibility_mode = PUBLIC obbligatorio
         comparsa/figurante, nessuna Agency
         visibile nel DB pubblico
         importabile nel DB privato di Cast Agency
```

### Visibilità e contratti

```
talent_agency_visibility — gestisce due flussi:
  1. ACTOR → AGENCY   con contract_type (EXCLUSIVE / NON_EXCLUSIVE)
  2. EXTRA → CAST_AGENCY  senza contract_type (è importazione, non contratto)

Regole applicative (non SQL):
  EXCLUSIVE → max 1 Agency ACCEPTED per talento
  NON_EXCLUSIVE → più Agency ACCEPTED possibili
  EXTRA → contract_type = null, solo CAST_AGENCY come destinazione
```

### Agency visibility policy

Ogni Agency sceglie se i propri attori sono visibili alle Cast Agency:
```
VISIBLE_TO_CAST_AGENCIES → Cast Agency vede profili e può mandare role_request
NOT_VISIBLE → Cast Agency vede solo che l'Agency esiste
```

### Flusso Cast Agency → Agency (per attori)

```
Cast Agency carica copione PDF
    → AI Service (futuro) genera lo spoglio → crea roles nel progetto
    → per ruoli che require_actor = true: Cast Agency manda role_request ad Agency
    → Agency accetta e propone attori tramite role_request_proposals
    → Cast Agency sceglie chi convocare
```

---

## Prossimi step da implementare (in ordine)

1. **JWT Authentication** — `JwtTokenProvider`, `JwtAuthenticationFilter`,
   endpoint `POST /api/auth/login`, protezione endpoint con ruoli
2. **TalentProfile entity** — con `talent_type`, `visibility_mode`,
   tutti i campi fisici e competenze, `@PrePersist` per `updatedAt`
3. **ProAccount entity** — con `agency_visibility_policy`
4. **TalentAgencyVisibility** — logica di richiesta/accettazione con validazione
   del contract_type esclusivo
5. **PrivateDatabase + PrivateDbEntry** — DB privato per Agency e Cast Agency
6. **Project + Role + Application** — flusso di casting per comparse
7. **RoleRequest + RoleRequestProposal** — flusso Cast Agency → Agency
8. **Gamification** — Badge, TalentBadge, logica XP e rank

---

## Problemi già risolti — non ripetere questi errori

| Problema | Causa | Soluzione |
|---|---|---|
| `missing sequence [users_seq]` | `@GeneratedValue` su UUID con Hibernate 7 | Generare UUID in `@PrePersist` |
| `expression is of type character varying` | Enum PostgreSQL nativo senza tipo esplicito | `@JdbcType(PostgreSQLEnumJdbcType.class)` |
| Loop infinito in `@JsonCreator` | `valueOf()` richiama Jackson internamente | Iterare su `values()` con `equalsIgnoreCase` |
| Flyway non si attiva | Spring Boot 4.x richiede starter dedicato | Usare `spring-boot-starter-flyway` |
| Maven usa Java 17 invece di 21 | Oracle JDK ancora nel PATH | `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"` prima di `mvn` |

---

## Note operative

- Per avviare Maven nel terminale IntelliJ impostare sempre JAVA_HOME:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
  mvn clean compile
  ```
- L'applicazione gira su `http://localhost:8080`
- Credenziali DB: host `localhost`, porta `5432`, db `ciakme`, user `postgres`, password `0000`
- PostGIS è installato ma la colonna `location_geo` è commentata in V1 —
  aggiungere con `V2__add_postgis.sql` quando serve la ricerca geografica
