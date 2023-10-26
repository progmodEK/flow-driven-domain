-- Indexes for searching blocked order to OneOM
CREATE INDEX IF NOT EXISTS IDX_CHECKOUT_STATE ON checkout.checkout((checkout_data->>'state'));
CREATE INDEX IF NOT EXISTS IDX_CHECKOUT_CREATED_AT ON checkout.checkout((checkout_data->>'created_at'));
CREATE INDEX IF NOT EXISTS IDX_CHECKOUT_CART_ID ON checkout.checkout((checkout_data->>'cart_id'));


