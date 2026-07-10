CREATE SCHEMA IF NOT EXISTS checkout AUTHORIZATION "admin";

CREATE TABLE IF NOT EXISTS checkout.checkout (
  id uuid NOT NULL,
  data jsonb NOT NULL,
  CONSTRAINT checkout_pk PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS checkout.flow_task (
  id varchar NOT NULL,
  score int8 NOT NULL,
  status varchar NOT NULL,
  ver int4 NOT NULL,
  CONSTRAINT flow_task_pk PRIMARY KEY (id)
);
