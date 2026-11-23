# View pública do catálogo

## produtos.vw_pub_itens

Responsável: catálogo de Produtos e Serviços. Uso exclusivo para leitura analítica
(relatórios, agregações e dashboards), conforme §9.8. Operações de tela e criação
ou alteração de cadastro usam a API autenticada, não a view.

| Coluna | Tipo PostgreSQL | Significado |
|---|---|---|
| id | uuid | ID do produto; permanece estável |
| tenant_id | uuid | Empresa proprietária; filtro obrigatório pelo consumidor |
| sku | varchar(64) | Código do produto, único no tenant |
| nome | text | Nome do produto |
| categoria | text | Rótulo público, incluindo acentos e espaços |
| unidade | varchar(255) | Unidade comercial |
| periodicidade | varchar(255) | Periodicidade comercial |
| quantidade_minima | numeric(15,4) | Quantidade mínima |
| casas_decimais_quantidade | integer | 4 para GB, TB e HORA; 0 nas demais |
| ativo | boolean | Situação; itens inativos também aparecem na view |
| moeda | varchar(3) | BRL |
| preco_venda | numeric(15,2), nullable | Preço publicado; null sem precificação |
| fabricante_id | uuid, nullable | Referência ao fabricante |
| distribuidor_id | uuid, nullable | Referência ao distribuidor |
| updated_at | timestamptz | Maior timestamp entre produto e precificação |

O resultado inclui uma linha por produto. Não expõe custo, margem, comissão,
markup, preço mínimo, desconto máximo, autores, token público nem URL de imagem.
O consumidor sempre aplica `WHERE tenant_id = :tenantDoToken`; usa `ativo = true`
se quiser somente produtos ativos. A view não executa isolamento por tenant sozinha.

A V2 concede SELECT a `usr_chamados` se esse papel já existir. Novos consumidores
precisam de GRANT explícito pelo dono do schema; não recebem acesso às tabelas.
A V6 preserva os grants existentes ao recriar a view e converte tenant_id para UUID.
Essa mudança de tipo deve ser anunciada aos consumidores antes da implantação.

Para publicar no repositório compartilhado: `contratos/produtos.views.md`.
Remover ou renomear coluna publicada exige versão nova e coordenação com consumidores.
