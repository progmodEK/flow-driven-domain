CREATE TABLE IF NOT EXISTS checkout.checkout (
	id uuid NOT NULL,
	checkout_data jsonb NOT NULL,
	CONSTRAINT checkout_pkey PRIMARY KEY (id)
);

CREATE SEQUENCE IF NOT EXISTS checkout.startup_id_generator;

-- ---------------------------------------------------------------------
-- INDEXES CREATION MUST NOT BE DONE BY FLYWAY - USE JUST FOR HISTORY --
-- ---------------------------------------------------------------------
--CREATE INDEX IF NOT EXISTS IDX_ORDER_ID ON checkout.checkout ((checkout_data->'order'->>'order_id'));
--CREATE INDEX IF NOT EXISTS IDX_PROFILE_ID ON checkout.checkout ((checkout_data #>> '{profile,profile_id}'));
--CREATE INDEX IF NOT EXISTS IDX_STATE ON checkout.checkout ((checkout_data->>'state'));
--CREATE INDEX IF NOT EXISTS IDX_CREATED_AT ON checkout.checkout ((checkout_data->>'created_at'));