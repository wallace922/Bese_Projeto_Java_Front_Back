-- ============================================================
-- V7: Suporte a múltiplos impostos (Tax) por item de NP.
--
-- Estratégia: Expand → Migrate → Contract (padrão já usado em V1/V2/V3).
--
-- Esta migration é 100% ADITIVA — nenhuma linha existente é alterada
-- ou apagada. Seguro rodar em produção sem downtime.
--
-- O campo payment_note_item.tax_id NÃO é removido aqui.
-- Ele continua existindo e sincronizado durante toda a transição
-- (Fases 2 e 3). Será removido apenas na Fase 6 (contract),
-- semanas depois, após confirmar que nenhum consumidor depende dele.
-- ============================================================

-- 1) Adiciona a nova coluna de relacionamento em `tax`, apontando para o item.
--    NULLABLE: a coluna nova não quebra registros históricos (que ficam com NULL
--    até o backfill abaixo rodar).
ALTER TABLE tax
  ADD COLUMN payment_note_item_id BIGINT NULL;

-- 2) Cria a FK de tax → payment_note_item.
ALTER TABLE tax
  ADD CONSTRAINT fk_tax_payment_note_item
  FOREIGN KEY (payment_note_item_id) REFERENCES payment_note_item(id);

-- 3) Backfill: para cada payment_note_item com tax_id preenchido,
--    popula tax.payment_note_item_id com o ID do item correspondente.
--    Apenas leitura + update; nenhuma linha é apagada ou criada.
UPDATE tax t
JOIN payment_note_item pni ON pni.tax_id = t.id
SET t.payment_note_item_id = pni.id;

-- ============================================================
-- ROLLBACK (executar somente se necessário reverter):
--
--   ALTER TABLE tax DROP FOREIGN KEY fk_tax_payment_note_item;
--   ALTER TABLE tax DROP COLUMN payment_note_item_id;
--
-- ⚠️ O rollback não perde dados: payment_note_item.tax_id ainda
-- estará intacto (nunca foi tocado nesta migration).
-- ============================================================
