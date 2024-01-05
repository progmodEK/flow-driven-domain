CREATE SCHEMA IF NOT EXISTS poc AUTHORIZATION "admin";

CREATE TABLE IF NOT EXISTS poc.order_preparation (
	id uuid NOT NULL,
	data jsonb NOT NULL,
	CONSTRAINT order_preparation_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS poc.flow_task (
	id varchar NOT NULL,
	score int8 NOT NULL,
	status varchar NOT NULL,
	ver int4 NOT NULL,
	CONSTRAINT flow_task_pk PRIMARY KEY (id)
);

