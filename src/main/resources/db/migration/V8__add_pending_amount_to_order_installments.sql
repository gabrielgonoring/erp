-- ============================================================================
-- MIGRATION: ADICIONA COLUNA PENDING_AMOUNT EM ORDER_INSTALLMENTS
-- ============================================================================

ALTER TABLE order_installments
    ADD COLUMN pending_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 AFTER amount;

-- Atualiza as parcelas existentes calculando o valor pendente
UPDATE order_installments oi
LEFT JOIN (
    SELECT installment_id, SUM(amount_paid) AS total_paid
    FROM payment_transactions
    GROUP BY installment_id
) pt ON pt.installment_id = oi.id
SET oi.pending_amount = CASE
    WHEN oi.status = 'PAID' OR oi.status = 'CANCELED' THEN 0.00
    ELSE GREATEST(0.00, oi.amount - COALESCE(pt.total_paid, 0.00))
END;
