CREATE TABLE IF NOT EXISTS dict_transactions (
  id varchar(36) not null,
  transaction_id varchar(36),
  event_id varchar(36),
  event_type varchar(256),
  transaction_type varchar(256),
  transaction_external_id bigint,
  transaction_date datetime(3),
  entry_type varchar(256)
);