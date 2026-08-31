INSERT INTO payment_note_item (payment_note_id, description, value, tax_id)
SELECT pn.id, 'Item migrado automaticamente', pn.valor, pn.tax_id
FROM payment_note pn
WHERE pn.tax_id IS NOT NULL OR pn.valor IS NOT NULL;
