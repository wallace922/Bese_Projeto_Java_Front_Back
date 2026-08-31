-- Nullifica a coluna legada para garantir que não há referências órfãs
UPDATE payment_note SET tax_id = NULL WHERE tax_id IS NOT NULL;

-- Remove a FK constraint (nome dinâmico gerado pelo Hibernate)
SELECT IFNULL(
    (SELECT CONCAT('ALTER TABLE payment_note DROP FOREIGN KEY ', CONSTRAINT_NAME)
     FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'payment_note'
       AND COLUMN_NAME = 'tax_id'
       AND REFERENCED_TABLE_NAME = 'tax'
     LIMIT 1),
    'SELECT 1')
INTO @sql;

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove a coluna legada
ALTER TABLE payment_note DROP COLUMN tax_id;
