-- ============================================================
-- SCHEMA MÉTIER POUR LE MODULE AUTOMATISATIONZINO
-- ============================================================

-- ============================================================
-- 1. TABLE : fichiers_source
-- ============================================================
CREATE TABLE IF NOT EXISTS fichiers_source (
    id BIGSERIAL PRIMARY KEY,
    nom_fichier VARCHAR(255) NOT NULL UNIQUE,
    chemin_acces VARCHAR(500),
    statut VARCHAR(50) DEFAULT 'PENDING',
    date_creation TIMESTAMP,
    date_derniere_modification TIMESTAMP,
    tentative_envoi INTEGER DEFAULT 0,
    dernier_message_erreur TEXT,
    code_produit_principal VARCHAR(100),
    donnees_extraites_json TEXT,
    extraction_effectuee BOOLEAN DEFAULT FALSE
);

-- ============================================================
-- 2. TABLE : historique_envois
-- ============================================================
CREATE TABLE IF NOT EXISTS historique_envois (
    id BIGSERIAL PRIMARY KEY,
    nom_fichier VARCHAR(255) NOT NULL,
    statut VARCHAR(50),
    code_erreur VARCHAR(50),
    message_erreur TEXT,
    date_envoi TIMESTAMP,
    tentative INTEGER DEFAULT 0,
    reponse_api TEXT,
    temps_execution_ms BIGINT,
    code_produit_principal VARCHAR(100)
);

-- ============================================================
-- 3. TABLE : tickets_vente_zino (Optionnelle)
-- ============================================================
CREATE TABLE IF NOT EXISTS tickets_vente_zino (
    id BIGSERIAL PRIMARY KEY,
    num_ticket VARCHAR(50) NOT NULL,
    date_vente DATE,
    nom_client VARCHAR(255),
    code_produit_principal VARCHAR(100),
    designation_principale VARCHAR(500),
    mode_paiement VARCHAR(100),
    montant_ht DECIMAL(15, 2),
    tva DECIMAL(15, 2),
    montant_ttc DECIMAL(15, 2),
    num_compte_client VARCHAR(50),
    nom_fichier_source VARCHAR(255),
    date_import TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. TABLE : repartition_paiements (Optionnelle)
-- ============================================================
CREATE TABLE IF NOT EXISTS repartition_paiements (
    id BIGSERIAL PRIMARY KEY,
    date_repartition DATE,
    mode_paiement VARCHAR(100),
    total_montant_ht DECIMAL(15, 2),
    total_tva DECIMAL(15, 2),
    total_montant_ttc DECIMAL(15, 2),
    nombre_transactions INTEGER DEFAULT 0,
    designation_principale VARCHAR(500),
    date_import TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 5. INDEX
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_fichiers_statut ON fichiers_source(statut);
CREATE INDEX IF NOT EXISTS idx_fichiers_nom ON fichiers_source(nom_fichier);
CREATE INDEX IF NOT EXISTS idx_fichiers_date_creation ON fichiers_source(date_creation);
CREATE INDEX IF NOT EXISTS idx_fichiers_extraction ON fichiers_source(extraction_effectuee);

CREATE INDEX IF NOT EXISTS idx_historique_nom ON historique_envois(nom_fichier);
CREATE INDEX IF NOT EXISTS idx_historique_statut ON historique_envois(statut);
CREATE INDEX IF NOT EXISTS idx_historique_date ON historique_envois(date_envoi);
CREATE INDEX IF NOT EXISTS idx_historique_tentative ON historique_envois(tentative);

CREATE INDEX IF NOT EXISTS idx_tickets_num ON tickets_vente_zino(num_ticket);
CREATE INDEX IF NOT EXISTS idx_tickets_date ON tickets_vente_zino(date_vente);
CREATE INDEX IF NOT EXISTS idx_tickets_mode ON tickets_vente_zino(mode_paiement);

CREATE INDEX IF NOT EXISTS idx_repartition_mode ON repartition_paiements(mode_paiement);
CREATE INDEX IF NOT EXISTS idx_repartition_date ON repartition_paiements(date_repartition);

-- ============================================================
-- 6. COMMENTS (Documentation)
-- ============================================================
COMMENT ON TABLE fichiers_source IS 'Liste des fichiers source à traiter par le batch';
COMMENT ON COLUMN fichiers_source.nom_fichier IS 'Nom du fichier (utilisé pour la déduplication)';
COMMENT ON COLUMN fichiers_source.statut IS 'PENDING, DOWNLOADED, TRANSFORMED, SENT, ERROR, ECHEC_DEFINITIF';
COMMENT ON COLUMN fichiers_source.tentative_envoi IS 'Nombre de tentatives d''envoi';
COMMENT ON COLUMN fichiers_source.code_produit_principal IS 'Code produit extrait du nom du fichier';
COMMENT ON COLUMN fichiers_source.donnees_extraites_json IS 'Données extraites du fichier CSV stockées en JSON pour traçabilité et rejeu';
COMMENT ON COLUMN fichiers_source.extraction_effectuee IS 'Indique si les données ont été extraites et sauvegardées en JSON';

COMMENT ON TABLE historique_envois IS 'Historique des envois vers la FNE';
COMMENT ON COLUMN historique_envois.statut IS 'SUCCES, ECHEC, ECHEC_DEFINITIF, EN_COURS, EN_ATTENTE';
COMMENT ON COLUMN historique_envois.reponse_api IS 'Réponse complète de l''API FNE (JSON)';

COMMENT ON TABLE tickets_vente_zino IS 'Tickets de vente extraits des fichiers Zino';
COMMENT ON TABLE repartition_paiements IS 'Répartition des paiements par mode de paiement';

