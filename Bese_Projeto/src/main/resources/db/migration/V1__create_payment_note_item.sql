CREATE TABLE payment_note_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_note_id BIGINT NOT NULL,
    description VARCHAR(255),
    value DECIMAL(19,2) NOT NULL,
    tax_id BIGINT,
    CONSTRAINT fk_pni_payment_note FOREIGN KEY (payment_note_id) REFERENCES payment_note(id),
    CONSTRAINT fk_pni_tax FOREIGN KEY (tax_id) REFERENCES tax(id)
);
