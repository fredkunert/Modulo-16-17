-- Preparação de banco NOVO para desenvolvimento/CI, executada pelo PostgreSQL.
-- A infraestrutura integrada também entrega o schema vazio pronto ao módulo.
-- Não executar V1 aqui: somente Flyway cria tabelas e registra as migrations.
CREATE SCHEMA IF NOT EXISTS produtos;
