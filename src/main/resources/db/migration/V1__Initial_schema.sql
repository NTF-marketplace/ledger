-- order이 오면 ledger를 생성하는데 해당 데이터에 필요한

CREATE TABLE IF NOT EXISTS ledger (
    id SERIAL PRIMARY KEY,
    nft_id BIGINT NOT NULL,
    sele_address VARCHAR(255) NOT NULL,
    order_address VARCHAR(255) NOT NULL,
    created_at BIGINT not null,
    ledger_price DECIMAL(19, 4) NOT NULL,
    token_type token_type not null
);