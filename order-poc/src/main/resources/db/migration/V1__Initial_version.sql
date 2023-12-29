CREATE SCHEMA IF NOT EXISTS poc AUTHORIZATION "admin";

CREATE TABLE IF NOT EXISTS poc.flow_long (
	id bigint NOT NULL,
	flow_data jsonb NOT NULL,
	CONSTRAINT flow_long_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS poc.flow_uuid (
	id uuid NOT NULL,
	flow_data jsonb NOT NULL,
	CONSTRAINT flow_uuid_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS poc.flow_task (
	id varchar NOT NULL,
	score int8 NOT NULL,
	status varchar NOT NULL,
	ver int4 NOT NULL,
	CONSTRAINT flow_task_pk PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------
-- INDEXES CREATION MUST NOT BE DONE BY FLYWAY - USE JUST FOR HISTORY --
-- ---------------------------------------------------------------------
