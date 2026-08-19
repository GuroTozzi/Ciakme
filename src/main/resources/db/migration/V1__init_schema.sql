-- =============================================
-- EXTENSIONS
-- =============================================
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- ENUM TYPES
-- =============================================

CREATE TYPE user_role AS ENUM (
    'TALENT',
    'AGENCY',
    'CAST_AGENCY'
);

CREATE TYPE talent_type AS ENUM (
    'ACTOR',  -- appartiene ad una o più Agency
    'EXTRA'   -- comparsa, DB pubblico + Cast Agency
);

CREATE TYPE contract_type AS ENUM (
    'EXCLUSIVE',     -- una sola Agency
    'NON_EXCLUSIVE'  -- più Agency contemporaneamente
);

CREATE TYPE visibility_mode AS ENUM (
    'PUBLIC',     -- solo EXTRA
    'RESTRICTED'  -- solo ACTOR
);

CREATE TYPE agency_request_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED'
);

CREATE TYPE agency_visibility_policy AS ENUM (
    'VISIBLE_TO_CAST_AGENCIES',
    'NOT_VISIBLE'
);

CREATE TYPE talent_plan AS ENUM (
    'FREE',
    'PLUS',
    'PRO'
);

CREATE TYPE talent_rank AS ENUM (
    'EXTRA',
    'RISING',
    'FEATURED',
    'STAR',
    'ICON'
);

CREATE TYPE project_status AS ENUM (
    'DRAFT',
    'ACTIVE',
    'CASTING',
    'CLOSED'
);

CREATE TYPE application_status AS ENUM (
    'PENDING',
    'SHORTLISTED',
    'CONFIRMED',
    'REJECTED'
);

CREATE TYPE role_request_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'CLOSED'
);

CREATE TYPE media_type AS ENUM (
    'PHOTO',
    'VIDEO',
    'AUDIO'
);

CREATE TYPE gender_type AS ENUM (
    'MALE',
    'FEMALE',
    'NON_BINARY'
);

CREATE TYPE character_importance AS ENUM (
    'PROTAGONIST',
    'SECONDARY',
    'EXTRA'
);

-- =============================================
-- AUTH SERVICE DB
-- =============================================

CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          user_role    NOT NULL,
                       locale        VARCHAR(10)  NOT NULL DEFAULT 'it',
                       is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
                       last_login_at TIMESTAMP
);

-- =============================================
-- TALENT SERVICE DB
-- =============================================

CREATE TABLE talent_profiles (
                                 id                    UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
                                 user_id               UUID            NOT NULL UNIQUE,
                                 talent_type           talent_type     NOT NULL,
    -- ACTOR → RESTRICTED obbligatorio
    -- EXTRA → PUBLIC obbligatorio
                                 visibility_mode       visibility_mode NOT NULL,

    -- Identità
                                 fiscal_code           VARCHAR(16)     NOT NULL UNIQUE,
                                 identity_verified_at  TIMESTAMP,
                                 first_name            VARCHAR(100)    NOT NULL,
                                 last_name             VARCHAR(100)    NOT NULL,
                                 birth_date            DATE            NOT NULL,
                                 gender                gender_type     NOT NULL,
                                 nationality           VARCHAR(100)    NOT NULL,

    -- Fisico
                                 height_cm             INT,
                                 weight_kg             INT,
                                 hair_color            VARCHAR(50),
                                 eye_color             VARCHAR(50),
                                 build                 VARCHAR(50),
                                 ethnicity             JSONB,
                                 has_tattoos           BOOLEAN,
                                 has_piercings         BOOLEAN,
                                 clothing_size         VARCHAR(10),
                                 shoe_size             INT,

    -- Competenze e location
                                 languages             JSONB,
                                 skills                JSONB,
                                 city                  VARCHAR(100),
                                 location_geo          GEOGRAPHY(POINT, 4326),
                                 travel_radius_km      INT             NOT NULL DEFAULT 0,
                                 open_to_relocation    BOOLEAN         NOT NULL DEFAULT FALSE,
                                 open_to_international BOOLEAN         NOT NULL DEFAULT FALSE,
                                 union_membership      VARCHAR(100),

    -- Gamification
                                 plan                  talent_plan     NOT NULL DEFAULT 'FREE',
                                 xp_points             INT             NOT NULL DEFAULT 0,
                                 rank                  talent_rank     NOT NULL DEFAULT 'EXTRA',
                                 streak_days           INT             NOT NULL DEFAULT 0,
                                 last_activity_at      TIMESTAMP,
                                 profile_completeness  FLOAT           NOT NULL DEFAULT 0,

                                 created_at            TIMESTAMP       NOT NULL DEFAULT NOW(),
                                 updated_at            TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE talent_media (
                              id           UUID       PRIMARY KEY DEFAULT gen_random_uuid(),
                              talent_id    UUID       NOT NULL REFERENCES talent_profiles(id) ON DELETE CASCADE,
                              media_type   media_type NOT NULL,
                              storage_url  VARCHAR(500) NOT NULL,
                              is_primary   BOOLEAN    NOT NULL DEFAULT FALSE,
                              sort_order   INT        NOT NULL DEFAULT 0,
                              duration_sec INT,
                              uploaded_at  TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE TABLE availability_slots (
                                    id           UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                    talent_id    UUID      NOT NULL REFERENCES talent_profiles(id) ON DELETE CASCADE,
                                    date_from    TIMESTAMP NOT NULL,
                                    date_to      TIMESTAMP NOT NULL,
                                    is_available BOOLEAN   NOT NULL DEFAULT TRUE,
                                    note         VARCHAR(500)
);

-- =============================================
-- PRO SERVICE DB
-- =============================================

CREATE TABLE pro_accounts (
                              id                       UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id                  UUID                     NOT NULL UNIQUE,
                              org_name                 VARCHAR(255)             NOT NULL,
    -- policy visibilità attori verso Cast Agency
    -- rilevante solo per AGENCY, ignorato per CAST_AGENCY
                              agency_visibility_policy agency_visibility_policy NOT NULL DEFAULT 'NOT_VISIBLE',
                              vat_number               VARCHAR(50),
                              created_at               TIMESTAMP                NOT NULL DEFAULT NOW()
);

-- 1:1 con pro_account
-- sia AGENCY che CAST_AGENCY hanno il loro DB privato
CREATE TABLE private_databases (
                                   id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                   pro_id            UUID         NOT NULL UNIQUE,
                                   name              VARCHAR(255) NOT NULL,
    -- true: aggiornamento profilo pubblico si riflette
    --       automaticamente nel DB privato
                                   auto_sync_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
                                   created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Voci nel DB privato
-- AGENCY: contiene ACTOR accettati
-- CAST_AGENCY: contiene EXTRA importati dal pubblico
CREATE TABLE private_db_entries (
                                    id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                    db_id           UUID      NOT NULL REFERENCES private_databases(id) ON DELETE CASCADE,
                                    talent_id       UUID      NOT NULL,
                                    internal_note   TEXT,
                                    internal_rating INT       CHECK (internal_rating BETWEEN 1 AND 5),
                                    tags            JSONB,
                                    added_at        TIMESTAMP NOT NULL DEFAULT NOW(),
                                    UNIQUE(db_id, talent_id)
);

-- =============================================
-- VISIBILITY SERVICE DB
-- =============================================

-- Gestisce due flussi:
-- 1. ACTOR → AGENCY   (richiesta con contratto)
-- 2. EXTRA → CAST_AGENCY (richiesta di importazione nel DB privato)
CREATE TABLE talent_agency_visibility (
                                          id            UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
                                          talent_id     UUID                  NOT NULL,
                                          pro_id        UUID                  NOT NULL,
    -- EXCLUSIVE: max 1 Agency attiva
    -- NON_EXCLUSIVE: più Agency
    -- NULL per EXTRA verso CAST_AGENCY (non è un contratto formale)
                                          contract_type contract_type,
                                          status        agency_request_status NOT NULL DEFAULT 'PENDING',
                                          requested_at  TIMESTAMP             NOT NULL DEFAULT NOW(),
                                          responded_at  TIMESTAMP,
                                          UNIQUE(talent_id, pro_id)
);

-- =============================================
-- CASTING SERVICE DB
-- =============================================

-- I progetti sono creati dalle CAST_AGENCY
CREATE TABLE projects (
                          id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                          pro_id       UUID           NOT NULL,
                          title        VARCHAR(255)   NOT NULL,
                          project_type VARCHAR(100),
                          description  TEXT,
                          city         VARCHAR(100),
                          shoot_start  DATE,
                          shoot_end    DATE,
                          status       project_status NOT NULL DEFAULT 'DRAFT',
                          created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
                          updated_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Ruoli del film
-- generati dallo spoglio manuale o AI
CREATE TABLE roles (
                       id                    UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
                       project_id            UUID                 NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                       character_name        VARCHAR(255)         NOT NULL,
                       importance            character_importance NOT NULL,
                       description           TEXT,
                       physical_requirements JSONB,
                       skill_requirements    JSONB,
                       headcount_needed      INT                  NOT NULL DEFAULT 1,
                       headcount_confirmed   INT                  NOT NULL DEFAULT 0,
                       availability_from     DATE,
                       availability_to       DATE,
                       compensation_day      DECIMAL(10,2),
    -- true: ruolo da coprire con ACTOR tramite Agency
    -- false: ruolo da coprire con EXTRA
                       requires_actor        BOOLEAN              NOT NULL DEFAULT FALSE,
                       created_at            TIMESTAMP            NOT NULL DEFAULT NOW()
);

-- Candidature dirette degli EXTRA a ruoli comparsa
-- Gli ACTOR non si candidano direttamente:
-- vengono proposti dall'Agency tramite role_requests
CREATE TABLE applications (
                              id          UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
                              talent_id   UUID               NOT NULL,
                              role_id     UUID               NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                              status      application_status NOT NULL DEFAULT 'PENDING',
                              talent_note TEXT,
                              pro_note    TEXT,
                              applied_at  TIMESTAMP          NOT NULL DEFAULT NOW(),
                              updated_at  TIMESTAMP          NOT NULL DEFAULT NOW(),
                              UNIQUE(talent_id, role_id)
);

-- Richieste formali Cast Agency → Agency
-- per trovare attori adatti a ruoli specifici
CREATE TABLE role_requests (
                               id             UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
                               cast_agency_id UUID                NOT NULL,
                               agency_id      UUID                NOT NULL,
                               role_id        UUID                NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                               message        TEXT,
                               status         role_request_status NOT NULL DEFAULT 'PENDING',
                               requested_at   TIMESTAMP           NOT NULL DEFAULT NOW(),
                               responded_at   TIMESTAMP,
                               UNIQUE(cast_agency_id, agency_id, role_id)
);

-- Attori proposti dall'Agency in risposta
-- a una role_request accettata
CREATE TABLE role_request_proposals (
                                        id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                        role_request_id UUID      NOT NULL REFERENCES role_requests(id) ON DELETE CASCADE,
                                        talent_id       UUID      NOT NULL,
                                        note            TEXT,
                                        proposed_at     TIMESTAMP NOT NULL DEFAULT NOW(),
                                        UNIQUE(role_request_id, talent_id)
);

-- Risultato analisi AI del copione
-- l'AI Service analizza il PDF e genera i personaggi
-- la Cast Agency rivede e conferma
-- i ruoli vengono creati automaticamente nel progetto
CREATE TABLE script_analyses (
                                 id              UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                 project_id      UUID      NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                 raw_script_url  VARCHAR(500),
                                 analysis_result JSONB     NOT NULL,
                                 is_confirmed    BOOLEAN   NOT NULL DEFAULT FALSE,
                                 created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- GAMIFICATION SERVICE DB
-- =============================================

CREATE TABLE badges (
                        id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                        code        VARCHAR(50)  NOT NULL UNIQUE,
                        name        VARCHAR(100) NOT NULL,
                        description TEXT,
                        xp_reward   INT          NOT NULL DEFAULT 0,
                        icon_url    VARCHAR(500)
);

CREATE TABLE talent_badges (
                               id        UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                               talent_id UUID      NOT NULL,
                               badge_id  UUID      NOT NULL REFERENCES badges(id),
                               earned_at TIMESTAMP NOT NULL DEFAULT NOW(),
                               UNIQUE(talent_id, badge_id)
);

CREATE TABLE reviews (
                         id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                         reviewer_id UUID      NOT NULL,
                         talent_id   UUID      NOT NULL,
                         project_id  UUID,
                         rating      INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         body        TEXT,
                         is_public   BOOLEAN   NOT NULL DEFAULT FALSE,
                         created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- NOTIFICATION SERVICE DB
-- =============================================

CREATE TABLE notifications (
                               id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id    UUID         NOT NULL,
                               title      VARCHAR(255) NOT NULL,
                               body       TEXT         NOT NULL,
                               is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE email_logs (
                            id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                            to_email VARCHAR(255) NOT NULL,
                            subject  VARCHAR(500) NOT NULL,
                            template VARCHAR(100) NOT NULL,
                            status   VARCHAR(50)  NOT NULL DEFAULT 'SENT',
                            sent_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================
-- SEED — badge di default
-- =============================================
INSERT INTO badges (code, name, description, xp_reward) VALUES
                                                            ('FIRST_CIAK',   'Primo Ciak',       'Prima candidatura inviata',        50),
                                                            ('IRON_WEEK',    'Iron Week',        'Streak di 7 giorni consecutivi',  100),
                                                            ('VOLTO_NOTO',   'Volto Noto',       '3 foto caricate sul profilo',      30),
                                                            ('VOCE_D_ORO',   'Voce d''Oro',      'Campione vocale caricato',         30),
                                                            ('VERIFICATO',   'Verificato',       'Identità verificata',              75),
                                                            ('GLOBETROTTER', 'Globetrotter',     'Disponibile a trasferte estere',   40),
                                                            ('TOP_REFERRER', 'Top Referrer',     '5 amici invitati e registrati',    80),
                                                            ('IRON_MONTH',   '30 giorni',        'Streak di 30 giorni consecutivi', 200),
                                                            ('LEGGENDA',     'Leggenda del Set', 'Raggiunto il rank Icon',          500);

-- =============================================
-- INDICI per performance
-- =============================================
CREATE INDEX idx_talent_type         ON talent_profiles(talent_type);
CREATE INDEX idx_talent_visibility   ON talent_profiles(visibility_mode);
CREATE INDEX idx_talent_fiscal_code  ON talent_profiles(fiscal_code);
CREATE INDEX idx_talent_city         ON talent_profiles(city);
CREATE INDEX idx_talent_rank         ON talent_profiles(rank DESC, updated_at DESC);
CREATE INDEX idx_talent_xp           ON talent_profiles(xp_points DESC);
CREATE INDEX idx_visibility_talent   ON talent_agency_visibility(talent_id);
CREATE INDEX idx_visibility_pro      ON talent_agency_visibility(pro_id);
CREATE INDEX idx_visibility_status   ON talent_agency_visibility(status);
CREATE INDEX idx_db_entries_talent   ON private_db_entries(talent_id);
CREATE INDEX idx_applications_talent ON applications(talent_id);
CREATE INDEX idx_applications_role   ON applications(role_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_role_req_cast       ON role_requests(cast_agency_id);
CREATE INDEX idx_role_req_agency     ON role_requests(agency_id);
CREATE INDEX idx_role_req_status     ON role_requests(status);
CREATE INDEX idx_notifications_user  ON notifications(user_id, is_read);