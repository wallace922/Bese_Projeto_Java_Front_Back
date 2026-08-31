-- ============================================================
-- V8: Remove a constraint de FK que bloqueia a deleção de Tax.
--
-- Contexto:
--   A migration V7 adicionou a nova FK `tax.payment_note_item_id`,
--   invertendo o dono do relacionamento (OneToOne → ManyToOne).
--   Com isso, o Hibernate parou de gerenciar `payment_note_item.tax_id`
--   — mas a FK `fk_pni_tax` continuou existindo no banco, bloqueando
--   qualquer DELETE em `tax` enquanto `payment_note_item.tax_id` ainda
--   apontar para aquela linha.
--
-- Solução (ainda dentro do padrão Expand → Migrate → Contract):
--   Remove apenas a CONSTRAINT — a coluna `tax_id` é mantida intacta
--   para compatibilidade de leitura durante a transição.
--   A coluna em si só será removida na Fase 6 (contract), junto com
--   toda a limpeza do modelo legado.
--
-- Operação 100% aditiva/relaxante: nenhuma linha é alterada ou apagada.
-- Seguro rodar em produção sem downtime.
-- ============================================================

ALTER TABLE payment_note_item
  DROP FOREIGN KEY fk_pni_tax;

-- A coluna tax_id permanece — serve como backup de leitura enquanto
-- o frontend legado ainda pode ler o campo `tax` singular via API.
-- Será removida na Fase 6 (contract) após confirmar que nenhum
-- consumidor depende mais dela.

-- ============================================================
-- ROLLBACK (executar somente se necessário reverter):
--
--   ALTER TABLE payment_note_item
--     ADD CONSTRAINT fk_pni_tax
--     FOREIGN KEY (tax_id) REFERENCES tax(id);
--
-- ⚠️ O rollback só é possível se todos os `tax_id` ainda apontarem
-- para registros válidos em `tax` (sem órfãos).
-- ============================================================
