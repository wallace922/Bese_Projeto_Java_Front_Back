-- ============================================================
-- V5: Adiciona referência opcional de Empresa (beneficiário)
-- em cada item da NP.
--
-- Coluna NULLABLE — item sem valor aqui continua herdando o
-- CNPJ da própria NP (regra de negócio aplicada na camada de
-- serviço, não no banco).
--
-- Operação 100% aditiva. Seguro rodar em produção sem downtime.
-- ============================================================

-- 1) Adiciona a coluna nullable (sem DEFAULT obrigatório)
ALTER TABLE payment_note_item
  ADD COLUMN empresa_beneficiaria BIGINT NULL;

-- 2) Cria a FK para a tabela empresa (já existente)
ALTER TABLE payment_note_item
  ADD CONSTRAINT fk_pni_empresa_beneficiaria
  FOREIGN KEY (empresa_beneficiaria) REFERENCES empresa(id);

-- ============================================================
-- ROLLBACK (executar somente se necessário reverter):
--
--   ALTER TABLE payment_note_item DROP FOREIGN KEY fk_pni_empresa_beneficiaria;
--   ALTER TABLE payment_note_item DROP COLUMN empresa_beneficiaria;
-- ============================================================
