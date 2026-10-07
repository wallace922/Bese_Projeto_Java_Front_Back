-- ============================================================
-- V9: Trilha de auditoria (quem criou / quem atualizou).
--
-- Adiciona colunas NULLABLE em todas as tabelas de negócio.
-- NULLABLE de propósito: registros históricos ficam com NULL
-- (autoria desconhecida) e o ddl-auto=validate continua passando.
-- A partir desta versão, creates preenchem created_by/updated_by
-- e updates preenchem updated_by (service/Audit.java).
--
-- Sem FK para `user`: evita travar DELETE de usuário por causa
-- de registros históricos que ele criou.
--
-- Operação 100% aditiva. Seguro rodar em produção sem downtime.
-- ============================================================

ALTER TABLE payment_note ADD COLUMN created_by BIGINT NULL;
ALTER TABLE payment_note ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE empresa ADD COLUMN created_by BIGINT NULL;
ALTER TABLE empresa ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE empenho ADD COLUMN created_by BIGINT NULL;
ALTER TABLE empenho ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE financial_planning ADD COLUMN created_by BIGINT NULL;
ALTER TABLE financial_planning ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE payment_note_empenho ADD COLUMN created_by BIGINT NULL;
ALTER TABLE payment_note_empenho ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE tax_rule ADD COLUMN created_by BIGINT NULL;
ALTER TABLE tax_rule ADD COLUMN updated_by BIGINT NULL;
