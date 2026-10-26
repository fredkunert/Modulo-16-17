-- Suporte à API de precificação (pasta backendprecificacao), que grava na
-- tabela compartilhada produtos.precificacoes criada na V1.
-- Este serviço (catálogo) é o dono de TODAS as migrations do schema produtos:
-- o serviço de precificação roda com Flyway desligado.

-- Moeda da precificação (a V1 não tinha). Valor padrão BRL para linhas existentes.
ALTER TABLE produtos.precificacoes
    ADD COLUMN moeda varchar(3) NOT NULL DEFAULT 'BRL' CHECK (moeda ~ '^[A-Z]{3}$');

-- Histórico de alterações campo a campo. Só o serviço de precificação escreve aqui.
CREATE TABLE produtos.precificacoes_historico (
    id uuid PRIMARY KEY,
    item_id uuid NOT NULL,
    tenant_id varchar(255) NOT NULL,
    campo_alterado varchar(64) NOT NULL,
    valor_anterior text,
    valor_novo text,
    alterado_por varchar(255) NOT NULL,
    alterado_em timestamptz NOT NULL,
    FOREIGN KEY (tenant_id,item_id) REFERENCES produtos.itens(tenant_id,id)
);
CREATE INDEX ix_precificacoes_historico_item
    ON produtos.precificacoes_historico(tenant_id,item_id,alterado_em DESC);
