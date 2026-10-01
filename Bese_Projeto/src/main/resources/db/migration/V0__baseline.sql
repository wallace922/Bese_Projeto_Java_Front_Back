-- ============================================================
-- V0: Baseline do schema (banco zerado).
--
-- Contexto:
--   As migrations V1..V8 são patches incrementais sobre tabelas
--   que foram criadas fora do Flyway (via ddl-auto=create na
--   máquina original). Num banco novo e vazio, a V1 já quebra
--   (erro 1824: FK para payment_note/tax inexistentes) e o
--   ddl-auto=validate impede o Hibernate de criar o que falta.
--
-- Esta V0 recria o schema EXATAMENTE como ele era ANTES da V1,
-- ou seja, no formato legado esperado pelas migrations seguintes:
--   - payment_note COM as colunas legadas `valor` e `tax_id`
--     (a V2 faz backfill a partir delas, a V3 remove `tax_id`);
--   - tax SEM `payment_note_item_id` (adicionado na V7);
--   - tax_rule COM a unique antiga (cod_efd, data_inicio_vigencia)
--     (a V4 troca pela nova unique com codigo_receita);
--   - payment_note_item SEM `empresa_beneficiaria` (adicionada na V5).
--
-- NÃO cria (para não conflitar):
--   - payment_note_item  -> criada na V1
--   - user               -> criada na V6
--
-- NOMES DE COLUNA:
--   O Spring Boot aplica CamelCaseToUnderscoresNamingStrategy em
--   TODOS os nomes (inclusive nos @Column/@JoinColumn explícitos).
--   Ex: @Column("FontDeOrigin") vira `font_de_origin` no banco
--   (comprovado pelo erro de validate do Hibernate). Por isso esta
--   V0 já usa os nomes físicos em snake_case. Tipos seguem o DDL
--   que o Hibernate/MySQL geraria das entities.
-- ============================================================

-- 1) empresa (referenciada por payment_note e, na V5, por payment_note_item)
CREATE TABLE empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NULL,
    cnpj VARCHAR(14) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2) tax (referenciada pela V1 e pela coluna legada payment_note.tax_id)
--    `tipo` é enum SEM @Enumerated -> Hibernate persiste como ORDINAL (INT).
CREATE TABLE tax (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo INT NULL,
    cod_efd_used INT NULL,
    codigo_receita INT NULL,
    tax_rule_description VARCHAR(300) NULL,
    tax_status VARCHAR(255) NOT NULL,
    calculated_items JSON NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3) payment_note (formato legado: com `valor` e `tax_id`)
CREATE TABLE payment_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_payment_note INT NULL,
    `dataliquidação` DATE NULL,
    empresa BIGINT NULL,
    date_payment DATE NULL,
    documento_origin VARCHAR(25) NULL,
    `valor` DECIMAL(19,2) NULL,
    `status` VARCHAR(255) NULL,
    tax_id BIGINT NULL,
    CONSTRAINT fk_payment_note_empresa FOREIGN KEY (empresa) REFERENCES empresa(id),
    CONSTRAINT fk_payment_note_tax FOREIGN KEY (tax_id) REFERENCES tax(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4) tax_rule (formato pré-V4: unique antiga sem codigo_receita)
CREATE TABLE tax_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cod_efd INT NOT NULL,
    codigo_receita INT NOT NULL,
    description VARCHAR(300) NULL,
    data_inicio_vigencia DATE NOT NULL,
    data_fim_vigencia DATE NULL,
    items JSON NOT NULL,
    CONSTRAINT uq_tax_rule_cod_efd_inicio UNIQUE (cod_efd, data_inicio_vigencia)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5) empenho
CREATE TABLE empenho (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empenho INT NULL,
    ano INT NULL,
    font_de_origin BIGINT NULL,
    internal_plan VARCHAR(11) NULL,
    nature INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6) financial_planning
CREATE TABLE financial_planning (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero INT NULL,
    `data_deliquidação` DATE NULL,
    vinculation INT NULL,
    origin INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7) payment_note_empenho (vínculo N:N entre NP, empenho e planejamento)
CREATE TABLE payment_note_empenho (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empenho_fk BIGINT NULL,
    paymentnote_fk BIGINT NULL,
    finacialplanning_fk BIGINT NULL,
    value DECIMAL(19,2) NULL,
    CONSTRAINT fk_pne_empenho FOREIGN KEY (empenho_fk) REFERENCES empenho(id),
    CONSTRAINT fk_pne_payment_note FOREIGN KEY (paymentnote_fk) REFERENCES payment_note(id),
    CONSTRAINT fk_pne_financial_planning FOREIGN KEY (finacialplanning_fk) REFERENCES financial_planning(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
