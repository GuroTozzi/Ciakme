-- Tabelle geografiche: regioni, province, comuni italiani.
-- Popolate dalla migrazione successiva (V101__seed_geo_data.sql).

CREATE TABLE regioni (
                         id INTEGER PRIMARY KEY,
                         nome VARCHAR(100) NOT NULL,
                         latitudine DECIMAL(9,6),
                         longitudine DECIMAL(9,6)
);

CREATE TABLE province (
                          id INTEGER PRIMARY KEY,
                          nome VARCHAR(100) NOT NULL,
                          sigla VARCHAR(2),
                          regione_id INTEGER NOT NULL REFERENCES regioni(id),
                          latitudine DECIMAL(9,6),
                          longitudine DECIMAL(9,6)
);

CREATE TABLE comuni (
                        id INTEGER PRIMARY KEY,
                        nome VARCHAR(150) NOT NULL,
                        provincia_id INTEGER NOT NULL REFERENCES province(id),
                        capoluogo_provincia BOOLEAN NOT NULL DEFAULT FALSE,
                        codice_catastale VARCHAR(4),
                        latitudine DECIMAL(9,6),
                        longitudine DECIMAL(9,6)
);

CREATE INDEX idx_comuni_nome ON comuni (nome);