ALTER TABLE produtos.itens ADD COLUMN token_publico UUID;
ALTER TABLE produtos.itens ADD CONSTRAINT uk_itens_token_publico UNIQUE (token_publico);
