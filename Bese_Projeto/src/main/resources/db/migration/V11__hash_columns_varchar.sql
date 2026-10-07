-- ============================================================
-- V11: Ajusta o tipo das colunas de índice cego.
--
-- A V10 criou *_hash como CHAR(64), mas o Hibernate mapeia
-- String(length=64) para VARCHAR(64) e o ddl-auto=validate
-- reprova a divergência (CHAR vs VARCHAR). Padroniza para
-- VARCHAR(64), sem perda (hash hexadecimal sempre tem 64 chars).
--
-- Operação 100% aditiva. Seguro rodar em produção sem downtime.
-- ============================================================

ALTER TABLE `user` MODIFY COLUMN cpf_hash VARCHAR(64) NULL;
ALTER TABLE empresa MODIFY COLUMN cnpj_hash VARCHAR(64) NULL;
