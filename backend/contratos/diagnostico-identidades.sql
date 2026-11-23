-- Somente leitura. Execute no banco atual antes de implantar a V6.
-- Retorna aliases legados distintos, sem preços, senhas nem tokens.
WITH identidades AS (
 SELECT 'fabricantes' origem,'tenant_id' campo,tenant_id::text valor FROM produtos.fabricantes
 UNION ALL SELECT 'fabricantes','created_by',created_by::text FROM produtos.fabricantes
 UNION ALL SELECT 'fabricantes','updated_by',updated_by::text FROM produtos.fabricantes
 UNION ALL SELECT 'distribuidores','tenant_id',tenant_id::text FROM produtos.distribuidores
 UNION ALL SELECT 'distribuidores','created_by',created_by::text FROM produtos.distribuidores
 UNION ALL SELECT 'distribuidores','updated_by',updated_by::text FROM produtos.distribuidores
 UNION ALL SELECT 'itens','tenant_id',tenant_id::text FROM produtos.itens
 UNION ALL SELECT 'itens','created_by',created_by::text FROM produtos.itens
 UNION ALL SELECT 'itens','updated_by',updated_by::text FROM produtos.itens
 UNION ALL SELECT 'precificacoes','tenant_id',tenant_id::text FROM produtos.precificacoes
 UNION ALL SELECT 'precificacoes','created_by',created_by::text FROM produtos.precificacoes
 UNION ALL SELECT 'precificacoes','updated_by',updated_by::text FROM produtos.precificacoes
 UNION ALL SELECT 'precificacoes_historico','tenant_id',tenant_id::text FROM produtos.precificacoes_historico
 UNION ALL SELECT 'precificacoes_historico','alterado_por',alterado_por::text FROM produtos.precificacoes_historico
)
SELECT origem,campo,valor,count(*) AS ocorrencias FROM identidades
WHERE valor !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
GROUP BY origem,campo,valor ORDER BY origem,campo,valor;
