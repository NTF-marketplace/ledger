-- order이 오면 ledger를 생성하는데 해당 데이터에 필요한

CREATE TYPE  chain_type AS ENUM (
    'ETHEREUM_MAINNET',
    'LINEA_MAINNET',
    'LINEA_SEPOLIA',
    'POLYGON_MAINNET',
    'ETHEREUM_HOLESKY',
    'ETHEREUM_SEPOLIA',
    'POLYGON_AMOY'
    );

CREATE TABLE IF NOT EXISTS ledger (
    id SERIAL PRIMARY KEY,
    nft_id BIGINT NOT NULL,
    sale_address VARCHAR(255) NOT NULL,
    order_address VARCHAR(255) NOT NULL,
    created_at BIGINT not null,
    ledger_price DECIMAL(19, 4) NOT NULL,
    chain_type chain_type not null,
    order_id BIGINT UNIQUE
);

CREATE TABLE IF NOT EXISTS ledger_fail_log (
    id SERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    message VARCHAR(255)
    );
