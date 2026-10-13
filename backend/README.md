**Teste sem o login do outro grupo: [TESTE-LOCAL.md](TESTE-LOCAL.md).**

# Catálogo de produtos — versão corrigida

Projeto Java 21 / Spring Boot 3.3.4, baseado no código enviado. Abrange os 15 endpoints de catálogo, fabricantes e distribuidores, mais `/api/produtos/health`.

## Abrir e executar

1. Extraia o ZIP e abra esta pasta como projeto Maven no IntelliJ.
2. Selecione o JDK 21 e recarregue as dependências Maven.
3. Crie um banco PostgreSQL vazio de desenvolvimento. O usuário configurado precisa conseguir criar o schema `produtos`, tabelas e views.
4. Configure as variáveis abaixo no terminal ou em **Run > Edit Configurations > Environment variables**.
5. Execute `mvn spring-boot:run` ou a classe `CatalogoApplication`. A porta padrão é **8083**.

Exemplo PowerShell; substitua os valores entre `<...>` pelos dados da sua equipe:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/integrador'
$env:DB_USER = 'own_produtos'
$env:DB_PASSWORD = '<senha-do-banco>'
$env:JWT_ISSUER = '<emissor-exato-dos-tokens>'
$env:JWT_JWKS_URI = '<url-das-chaves-publicas-JWKS>'
$env:JWT_AUDIENCE = '<audiencia-definida-pelo-grupo-de-autenticacao>'
$env:JWT_PERMISSIONS_CLAIM = 'permissions'
mvn spring-boot:run
```

`JWT_PERMISSIONS_CLAIM` é configurável porque o PDF não define o nome desse claim. O padrão `permissions` espera uma lista de strings, por exemplo `['produtos.item.criar']` (no JSON real, use aspas duplas), ou uma string de permissões separadas por espaços. Confirme o nome e o formato com o grupo de autenticação. Não há usuário/senha local nem modo de produção que dispense JWT.

O serviço verifica assinatura RS256, emissor, audiência, expiração e identidade. Usuários usam `sub` como autor e `tenant_id` como tenant; o header `X-Tenant-Id` é ignorado para usuários. Tokens com `sub` iniciado por `svc:` exigem `clientId` e o header `X-Tenant-Id`; `clientId` é o autor auditado. As permissões são independentes.

## Endpoints

Todas as rotas abaixo começam com `/api/produtos`.

| Método | Rota | Permissão |
|---|---|---|
| POST | `/itens` | `produtos.item.criar` |
| GET | `/itens` | `produtos.item.ver` |
| GET | `/itens/{id}` | `produtos.item.ver` |
| PATCH | `/itens/{id}` | `produtos.item.editar` |
| GET | `/itens/{id}/resumo` | `produtos.item.ver_resumo` |
| GET | `/itens/resumo?ids=uuid1,uuid2` | `produtos.item.ver_resumo` |
| GET | `/busca?q=backup` | `produtos.item.ver_resumo` |
| GET | `/fabricantes` | `produtos.item.ver` |
| GET | `/fabricantes/{id}` | `produtos.item.ver` |
| POST | `/fabricantes` | `produtos.item.criar` |
| PATCH | `/fabricantes/{id}` | `produtos.item.editar` |
| GET | `/distribuidores` | `produtos.item.ver` |
| GET | `/distribuidores/{id}` | `produtos.item.ver` |
| POST | `/distribuidores` | `produtos.item.criar` |
| PATCH | `/distribuidores/{id}` | `produtos.item.editar` |

GET `/health` é público e verifica conectividade com o banco. As rotas antigas `/api/itens`, `/distribuidors`, PUT de item, `/inativar`, `/reativar` e `/disponibilidade` não são utilizadas.

As listagens aceitam `pagina=0&tamanho=20&ordenar=nome,asc`. Tamanho: 1 a 100; ordenação: `nome,asc` ou `nome,desc`, com desempate por ID. Itens também aceitam `q`, `categoria`, `fabricanteId`, `distribuidorId` e `ativo`. O lote admite de 1 a 100 UUIDs distintos, preserva a ordem e retorna `itens` e `idsNaoEncontrados`. A busca global retorna somente ativos, até cinco resultados, sem paginação.

## Cadastrar um produto

Envie `Authorization: Bearer <token>` e `Content-Type: application/json`:

```json
{
  "sku": "BKP-001",
  "nome": "Licença de backup",
  "fabricanteId": null,
  "distribuidorId": null,
  "categoria": "LICENÇA",
  "descricao": "Backup por usuário",
  "unidade": "USUARIO",
  "periodicidade": "MENSAL",
  "quantidadeMinima": "1.0000",
  "ativo": true
}
```

Na criação, `ativo` omitido ou nulo assume `true`. O SKU é armazenado sem espaços externos e em maiúsculas. Duplicidade é verificada por tenant, inclusive entre inativos, no serviço e por índice único no banco.

Categorias JSON seguem a grafia da especificação fornecida: `SOFTWARE`, `LICENÇA`, `SERVIÇO`, `PROJETO`, `CONSULTORIA`, `BACKUP`, `ARMAZENAMENTO`, `CLOUD`, `SEGURANÇA`, `ANTIVÍRUS`, `EDR`, `XDR`, `MICROSOFT 365`, `GOOGLE WORKSPACE`, `SUPORTE`, `OUTROS`. As constantes Java/banco usam nomes sem acentos e com `_`; a conversão é explícita. Confira essa grafia contra o OpenAPI efetivamente usado pela equipe antes da integração, pois esse arquivo não veio no ZIP.

Unidades: `USUARIO`, `LICENCA`, `ENDPOINT`, `SERVIDOR`, `VM`, `GB`, `TB`, `CAIXA_POSTAL`, `DOMINIO`, `HORA`, `UNIDADE`. Periodicidades: `UNICA`, `MENSAL`, `TRIMESTRAL`, `SEMESTRAL`, `ANUAL`.

`quantidadeMinima` é string decimal com ponto. GB, TB e HORA aceitam até quatro casas significativas após o ponto; demais unidades exigem quantidades inteiras (`"1.0000"` equivale a 1). A quantidade deve ser positiva e caber em `numeric(15,4)`; não há arredondamento. A resposta informa `casasDecimaisQuantidade`.

## Atualizar e inativar

PATCH recebe somente os campos que deseja alterar, com pelo menos um campo:

```json
{"nome":"Nome corrigido"}
```

```json
{"ativo":false}
```

```json
{"fabricanteId":null,"distribuidorId":null,"descricao":null}
```

Campos omitidos permanecem intactos. `null` remove somente referências opcionais e descrição; não remove nome, SKU, categoria, unidade, periodicidade, quantidade ou situação. Mudanças de unidade revalidam a quantidade existente. Fabricantes/distribuidores devem existir no mesmo tenant e estar ativos para novos vínculos. Inativação posterior preserva os vínculos anteriores.

Para fabricantes/distribuidores, POST recebe `{"nome":"Nome","ativo":true}`; PATCH aceita `nome` e/ou `ativo`. Não há DELETE.

Respostas usam `success`, `data`, `message` e `errors`. Cadastros retornam 201 com `Location`; consultas/edições retornam 200. Erros de formato recebem 400; quantidade não positiva e novo vínculo com parceiro inativo, 422; SKU duplicado, 409; recurso inexistente ou de outro tenant, 404; token inválido, 401; falta de permissão, 403.

## Integração com precificação e banco compartilhado

Esta entrega não implementa PUT/GET restrito de precificação nem seu histórico. O pacote `precificacao` é uma **projeção de leitura** da tabela compartilhada: consulta `preco_venda`, `preco_minimo` e `desconto_maximo`. Sem precificação, esses valores permanecem `null`, nunca zero. Cadastro/PATCH rejeitam campos financeiros com 400. Custos, margem, markup, impostos e comissão não são expostos.

`V1__catalogo.sql` cria as três tabelas cadastrais e a estrutura compartilhada de `precificacoes` necessária à leitura. `V2__view_publicada.sql` cria `produtos.vw_pub_itens`, sem dados sensíveis ou limites comerciais, e concede leitura à role `usr_chamados` se ela existir. O serviço de relatórios continua responsável por filtrar o tenant autenticado.

As migrations são uma base para **banco vazio**. Antes de integrar ao repositório comum, alinhe a numeração e a criação de `precificacoes` com seu colega: não duplique uma migration/tabela já existente. Não convertem automaticamente o banco antigo com `item`, `empresa_id`, `distribuidors`, categoria UUID e unidades MES/ANO. Dados legados precisam de migração própria e revisão. Execute as migrations com o proprietário do schema. Se `usr_chamados` for criada depois, será necessária uma nova migration de concessão.

## Testes

```powershell
mvn test
mvn package
```

Os testes padrão usam H2 em memória, sem precisar de PostgreSQL nem token real, com autenticação simulada pelo Spring Security Test. Cobrem as rotas, permissões, isolamento, auditoria, PATCH, precisão, vínculos, resumo, lote e busca. Há testes separados dos validadores de claims JWT.

Para executar os testes HTTP também em um PostgreSQL **exclusivo e descartável**, aplique as migrations reais por Flyway com estas propriedades:

```powershell
mvn test '-Dspring.datasource.url=jdbc:postgresql://localhost:5432/catalogo_test' '-Dspring.datasource.username=usuario_teste' '-Dspring.datasource.password=senha_teste' '-Dspring.flyway.enabled=true' '-Dspring.jpa.hibernate.ddl-auto=validate'
```

Os testes limpam as tabelas de negócio antes de cada caso. Nunca aponte este comando para banco com dados que deseja preservar. Eles não homologam o provedor JWT da equipe nem o módulo de precificação do colega.

Referências técnicas: [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/6.3/servlet/oauth2/resource-server/jwt.html) e [Spring MVC — RequestBody](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html).
