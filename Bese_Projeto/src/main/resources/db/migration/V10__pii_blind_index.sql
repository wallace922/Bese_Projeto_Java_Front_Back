-- ============================================================
-- V10: Índice cego (HMAC) para CPF/CNPJ + alargamento das colunas.
--
-- A partir desta versão o CPF (`user.cpf`) e o CNPJ
-- (`empresa.cnpj`) são cifrados em repouso (AES-256/GCM via
-- CpfConverter/CnpjConverter). Como cifra com IV aleatório não
-- permite busca exata, as buscas passam a usar as colunas de hash:
--   - user.cpf_hash / empresa.cnpj_hash (HMAC-SHA256 dos dígitos)
--
-- Linhas legadas (valor em claro, hash NULL): continuam funcionando.
-- Os services fazem fallback para a busca legada e regravam cifrado
-- + hash no primeiro uso (login/busca). Múltiplos NULLs são aceitos
-- nas uniques abaixo pelo MySQL.
--
-- Operação 100% aditiva. Seguro rodar em produção sem downtime.
-- ============================================================

-- user.cpf precisa caber o envelope Base64 (IV + texto + tag GCM)
ALTER TABLE `user` MODIFY COLUMN cpf VARCHAR(255) NOT NULL;
ALTER TABLE `user` ADD COLUMN cpf_hash CHAR(64) NULL;
ALTER TABLE `user` ADD CONSTRAINT uq_user_cpf_hash UNIQUE (cpf_hash);

ALTER TABLE empresa MODIFY COLUMN cnpj VARCHAR(255) NULL;
ALTER TABLE empresa ADD COLUMN cnpj_hash CHAR(64) NULL;
ALTER TABLE empresa ADD CONSTRAINT uq_empresa_cnpj_hash UNIQUE (cnpj_hash);
