-- ============================================================
-- V4: Permite que um mesmo cod_efd tenha múltiplas linhas de
-- TaxRule (uma por codigo_receita) vigentes na mesma data.
--
-- Operação 100% aditiva/relaxante: nenhuma linha existente é
-- alterada ou apagada. É seguro rodar em produção sem downtime.
-- ============================================================

-- 1) Remove a constraint antiga (cod_efd + data_inicio_vigencia)
ALTER TABLE tax_rule DROP INDEX uq_tax_rule_cod_efd_inicio;

-- 2) Cria a nova constraint (cod_efd + codigo_receita + data_inicio_vigencia)
--    Mantém a unicidade real (mesmo EFD + mesma receita + mesma data),
--    mas agora permite receitas diferentes para o mesmo EFD na mesma data.
ALTER TABLE tax_rule
  ADD CONSTRAINT uq_tax_rule_cod_efd_receita_inicio
  UNIQUE (cod_efd, codigo_receita, data_inicio_vigencia);

-- ============================================================
-- ROLLBACK (executar somente se necessário reverter):
--
--   ALTER TABLE tax_rule DROP INDEX uq_tax_rule_cod_efd_receita_inicio;
--   ALTER TABLE tax_rule
--     ADD CONSTRAINT uq_tax_rule_cod_efd_inicio
--     UNIQUE (cod_efd, data_inicio_vigencia);
--
-- ⚠️ O rollback só é possível se nenhum segundo codigo_receita
-- tiver sido cadastrado para o mesmo cod_efd/data. Verificar:
--
--   SELECT cod_efd, data_inicio_vigencia, COUNT(*) c
--   FROM tax_rule GROUP BY cod_efd, data_inicio_vigencia HAVING c > 1;
-- ============================================================
