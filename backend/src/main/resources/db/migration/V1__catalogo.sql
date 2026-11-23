CREATE SCHEMA IF NOT EXISTS produtos;

CREATE TABLE produtos.fabricantes (
    id uuid PRIMARY KEY,
    nome text NOT NULL CHECK (length(trim(nome)) > 0),
    situacao boolean NOT NULL DEFAULT true,
    tenant_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(255) NOT NULL,
    deleted_at timestamptz,
    versao bigint NOT NULL DEFAULT 0,
    UNIQUE (tenant_id,id)
);
CREATE INDEX ix_fabricantes_tenant ON produtos.fabricantes(tenant_id);

CREATE TABLE produtos.distribuidores (
    id uuid PRIMARY KEY,
    nome text NOT NULL CHECK (length(trim(nome)) > 0),
    situacao boolean NOT NULL DEFAULT true,
    tenant_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(255) NOT NULL,
    deleted_at timestamptz,
    versao bigint NOT NULL DEFAULT 0,
    UNIQUE (tenant_id,id)
);
CREATE INDEX ix_distribuidores_tenant ON produtos.distribuidores(tenant_id);

CREATE TABLE produtos.itens (
    id uuid PRIMARY KEY,
    sku varchar(64) NOT NULL CHECK (length(trim(sku)) > 0),
    nome text NOT NULL CHECK (length(trim(nome)) > 0),
    fabricante_id uuid,
    distribuidor_id uuid,
    categoria varchar(255) NOT NULL CHECK (categoria IN ('SOFTWARE','LICENCA','SERVICO','PROJETO','CONSULTORIA','BACKUP','ARMAZENAMENTO','CLOUD','SEGURANCA','ANTIVIRUS','EDR','XDR','MICROSOFT_365','GOOGLE_WORKSPACE','SUPORTE','OUTROS')),
    descricao text,
    unidade varchar(255) NOT NULL CHECK (unidade IN ('USUARIO','LICENCA','ENDPOINT','SERVIDOR','VM','GB','TB','CAIXA_POSTAL','DOMINIO','HORA','UNIDADE')),
    periodicidade varchar(255) NOT NULL CHECK (periodicidade IN ('UNICA','MENSAL','TRIMESTRAL','SEMESTRAL','ANUAL')),
    quantidade_minima numeric(15,4) NOT NULL CHECK (quantidade_minima > 0),
    ativo boolean NOT NULL DEFAULT true,
    tenant_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(255) NOT NULL,
    deleted_at timestamptz,
    versao bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_itens_tenant_id UNIQUE (tenant_id,id),
    CONSTRAINT fk_itens_fabricante FOREIGN KEY (tenant_id,fabricante_id) REFERENCES produtos.fabricantes(tenant_id,id),
    CONSTRAINT fk_itens_distribuidor FOREIGN KEY (tenant_id,distribuidor_id) REFERENCES produtos.distribuidores(tenant_id,id),
    CHECK (unidade IN ('GB','TB','HORA') OR quantidade_minima=trunc(quantidade_minima)),
    CHECK (deleted_at IS NULL)
);
CREATE UNIQUE INDEX uk_itens_tenant_sku ON produtos.itens(tenant_id,lower(trim(sku)));
CREATE INDEX ix_itens_tenant ON produtos.itens(tenant_id);

CREATE TABLE produtos.precificacoes (
    id uuid PRIMARY KEY,
    item_id uuid NOT NULL,
    preco_venda numeric(15,2),
    preco_minimo numeric(15,2),
    desconto_maximo numeric(15,4) CHECK (desconto_maximo BETWEEN 0 AND 100),
    custo numeric(15,2),
    markup_valor numeric(15,4),
    markup_tipo varchar(255) CHECK (markup_tipo IN ('PERCENTUAL','FATOR')),
    margem numeric(15,4),
    impostos_valor numeric(15,4),
    impostos_tipo varchar(255) CHECK (impostos_tipo IN ('PERCENTUAL','VALOR')),
    comissao_valor numeric(15,4),
    comissao_tipo varchar(255) CHECK (comissao_tipo IN ('PERCENTUAL','VALOR')),
    tenant_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL,
    created_by varchar(255) NOT NULL,
    updated_at timestamptz NOT NULL,
    updated_by varchar(255) NOT NULL,
    deleted_at timestamptz,
    versao bigint NOT NULL DEFAULT 0,
    UNIQUE (tenant_id,item_id),
    FOREIGN KEY (tenant_id,item_id) REFERENCES produtos.itens(tenant_id,id),
    CHECK ((markup_valor IS NULL) = (markup_tipo IS NULL)),
    CHECK ((impostos_valor IS NULL) = (impostos_tipo IS NULL)),
    CHECK ((comissao_valor IS NULL) = (comissao_tipo IS NULL)),
    CHECK (impostos_tipo <> 'VALOR' OR impostos_valor=round(impostos_valor,2)),
    CHECK (comissao_tipo <> 'VALOR' OR comissao_valor=round(comissao_valor,2))
);
CREATE INDEX ix_precificacoes_tenant ON produtos.precificacoes(tenant_id);
