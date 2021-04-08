CREATE TABLE IF NOT EXISTS transactions (
  id varchar(36) not null,
  transaction_id varchar(36),
  event_id varchar(36),
  event_type varchar(256),
  transaction_type varchar(256),
  transaction_date datetime(3),
  amount numeric(15,2)
);