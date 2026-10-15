# Backend 2 — Precificação

Serviço Spring Boot (`br.edu.pi2026.produtos`) com a **precificação manual e o histórico de alterações** de cada item
do catálogo. Roda na porta **8084** e tem imagem Docker própria (`Dockerfile`).

## Como se encaixa com o catálogo (backend 1)

Os dois serviços usam **o mesmo banco** (`integrador`, schema `produtos`):

| | Catálogo (`backend/`) | Precificação (esta pasta) |
|---|---|---|
| Porta | 8083 | 8084 |
| Tabelas | `itens`, `fabricantes`, `distribuidores`, `precificacoes` (cria) | grava em `precificacoes` e em `precificacoes_historico` |
| Migrations (Flyway) | **dono**: `V1`, `V2`, `V3` | desligado (`spring.flyway.enabled=false`) |
| Lê do outro | `preco_venda` de `precificacoes` | só se o item existe (`itens`, somente leitura) |

- **Suba o catálogo antes**: é ele que cria e atualiza o schema. Mudança de tabela = nova migration em
  `backend/src/main/resources/db/migration/`, nunca aqui.
- A API usa `preco`, `margemPercentual` e `MONETARIO`; no banco são `preco_venda`, `margem` e `VALOR`.
  A tradução está em `Precificacao.java` e `TipoValorConverter.java`; o contrato da API não muda.
- `tenant_id` e o usuário são **texto** (ex.: `tenant-local`, `usuario-local`), igual ao catálogo.

## Estrutura

```
src/main/java/br/edu/pi2026/produtos/
├── ProdutosApplication.java
├── precificacao/   Precificacao, PrecificacaoHistorico, repositórios, Service, Controller, TipoValorConverter, dto/
├── item/           Item e ItemRepository (projeção SOMENTE LEITURA da tabela itens do catálogo)
├── common/         ApiResponse, ApiErro, PaginaResposta, exceções, GlobalExceptionHandler, TenantContext
└── config/         SegurancaJwtConfig (produção), SecurityConfig + DevAuthFilter (só profile local), CorsConfig
contratos/precificacao.yaml   contrato OpenAPI
PRECIFICACAO.md               regras de negócio e testes
requests.http                 roteiro de testes manuais (IntelliJ)
Dockerfile
```

## Rodar localmente (desenvolvimento)

1. **Banco** (na raiz do repositório): `docker compose -f docker-compose.dev.yml up -d db`
2. **Catálogo** (pasta `backend`), profile `local` — cria o schema e emite token de teste (ver `backend/TESTE-LOCAL.md`).
   Variáveis: `DB_URL=jdbc:postgresql://localhost:5432/integrador`, `DB_USER=own_produtos`, `DB_PASSWORD=produtos_dev`.
3. **Esta pasta**, profile `local`, com as mesmas variáveis de banco. No IntelliJ: Run Configuration de `ProdutosApplication`,
   *Active profiles* `local`, *Environment variables* `DB_URL=jdbc:postgresql://localhost:5432/integrador;DB_USER=own_produtos;DB_PASSWORD=produtos_dev`.
   Pelo terminal: `mvn spring-boot:run "-Dspring-boot.run.profiles=local"`.
4. Abra `requests.http` e rode os passos em ordem: ele pega o token, cria um item no catálogo e usa o `id` dele aqui.

> O profile `local` desliga a validação de token e escuta só em `127.0.0.1`. **Nunca use em container ou servidor.**

## Imagem Docker

```bash
docker build -t produtos-precificacao ./backend/backendprecificacao      # a partir da raiz do repositório
```

Variáveis de ambiente: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_ISSUER`, `JWT_JWKS_URI`, `JWT_AUDIENCE`
(opcionais: `PORT`, `JWT_PERMISSIONS_CLAIM`, `CORS_ALLOWED_ORIGINS`, `LOG_LEVEL`). Sem o profile `local`, o token é validado
contra o JWKS do serviço de identidade — as mesmas variáveis do catálogo, então o mesmo token vale nos dois.
Na raiz há um `docker-compose.dev.yml` que sobe banco + os dois serviços + um gateway em `http://localhost:8080`.

## Permissões que este serviço usa

Para cadastrar no serviço de identidade:

- `produtos.precificacao.ver`
- `produtos.precificacao.editar`
- `produtos.precificacao.ver_valores_sensiveis` — **proposta**, ver `PRECIFICACAO.md`. Sem ela, custo/margem/markup/comissão voltam `null`.

## Endpoints da API de Precificação

Serviço de precificação, porta **8084** (`http://localhost:8084`). Pelo gateway de desenvolvimento (`docker-compose.dev.yml`) as mesmas rotas ficam em `http://localhost:8080`, junto com as do catálogo.
Contrato completo em OpenAPI: [`contratos/precificacao.yaml`](contratos/precificacao.yaml).

> Para testar localmente, rode com o profile `local` ativo (`SPRING_PROFILES_ACTIVE=local`).
> Nele um usuário fixo (`usuario-local`, tenant `tenant-local`) é autenticado automaticamente com todas as
> permissões, sem precisar de token. O `{itemId}` precisa ser de um item **criado no serviço de catálogo**
> com o mesmo tenant.

### Resumo

| Método | Rota | Permissão exigida | Descrição |
|--------|------|-------------------|-----------|
| `GET` | `/api/produtos/itens/{itemId}/precificacao` | `produtos.precificacao.ver` | Consulta a precificação vigente do item |
| `PUT` | `/api/produtos/itens/{itemId}/precificacao` | `produtos.precificacao.editar` | Cria ou atualiza a precificação do item |
| `GET` | `/api/produtos/itens/{itemId}/precificacao/historico` | `produtos.precificacao.ver` | Lista o histórico de alterações (paginado) |

`{itemId}` é o UUID de um item do catálogo.

### Formato das respostas

Toda resposta usa o envelope padrão da plataforma:

```json
{
  "success": true,
  "data": { },
  "message": null,
  "errors": []
}
```

Em caso de erro, `success` é `false`, `data` é `null`, `message` traz o resumo e `errors` traz a lista de problemas, cada um com `campo`, `codigo` e `detalhe`.

---

### `GET /api/produtos/itens/{itemId}/precificacao`

Consulta a precificação vigente.

**Resposta `200`**

```json
{
  "success": true,
  "data": {
    "itemId": "33333333-3333-3333-3333-333333333333",
    "moeda": "BRL",
    "custo": 100.00,
    "preco": 150.00,
    "margemPercentual": 20.00,
    "markupValor": 10.00,
    "markupTipo": "PERCENTUAL",
    "impostosValor": 8.00,
    "impostosTipo": "PERCENTUAL",
    "comissaoValor": 5.00,
    "comissaoTipo": "PERCENTUAL",
    "versao": 1,
    "updatedAt": "2026-10-15T02:11:00Z"
  },
  "message": null,
  "errors": []
}
```

---

### `PUT /api/produtos/itens/{itemId}/precificacao`

Cria a precificação do item ou atualiza a existente. Devolve o mesmo formato do `GET`.

**Body**

```json
{
  "moeda": "BRL",
  "custo": 100.00,
  "preco": 150.00,
  "margemPercentual": 20.00,
  "markupValor": 10.00,
  "markupTipo": "PERCENTUAL",
  "impostosValor": 8.00,
  "impostosTipo": "PERCENTUAL",
  "comissaoValor": 5.00,
  "comissaoTipo": "PERCENTUAL"
}
```

**Campos**

Todos os campos são opcionais e independentes. Campo não informado fica `null` (nunca é tratado como zero nem calculado automaticamente). Única exigência: **valor e tipo andam juntos** (`markupValor`+`markupTipo`, `impostosValor`+`impostosTipo`, `comissaoValor`+`comissaoTipo`): ambos preenchidos ou ambos em branco.

| Campo | Tipo | Regra |
|-------|------|-------|
| `moeda` | string | 3 letras maiúsculas (código ISO), ex.: `BRL` |
| `custo` | número | até 2 casas decimais |
| `preco` | número | até 2 casas decimais |
| `margemPercentual` | número | até 4 casas decimais |
| `markupValor` | número | até 4 casas decimais |
| `markupTipo` | string | `PERCENTUAL` ou `FATOR` |
| `impostosValor` | número | até 4 casas decimais |
| `impostosTipo` | string | `PERCENTUAL` ou `MONETARIO` (com `MONETARIO`, `impostosValor` aceita no máximo 2 casas) |
| `comissaoValor` | número | até 4 casas decimais |
| `comissaoTipo` | string | `PERCENTUAL` ou `MONETARIO` (com `MONETARIO`, `comissaoValor` aceita no máximo 2 casas) |

A API valida formato, precisão e a regra "valor e tipo juntos". Ela **não** confere se os valores são coerentes entre si (ex.: preço x margem).

---

### `GET /api/produtos/itens/{itemId}/precificacao/historico`

Lista o histórico de alterações do item, paginado.

**Query params**

| Parâmetro | Padrão | Descrição |
|-----------|--------|-----------|
| `pagina` | `0` | Número da página (começa em 0) |
| `tamanho` | `20` | Itens por página (máximo `100`) |

**Resposta `200`**

```json
{
  "success": true,
  "data": {
    "itens": [
      {
        "campoAlterado": "custo",
        "valorAnterior": 100.00,
        "valorNovo": 120.00,
        "alteradoPor": "usuario-local",
        "alteradoEm": "2026-10-15T02:15:00Z"
      }
    ],
    "pagina": 0,
    "tamanho": 20,
    "total": 1
  },
  "message": null,
  "errors": []
}
```

Campos decimais voltam como número e campos de tipo (`markupTipo`, etc.) voltam como texto.

---

### Valores sensíveis

Sem a permissão `produtos.precificacao.ver_valores_sensiveis`, os campos abaixo voltam `null`:

`custo`, `margemPercentual`, `markupValor`, `markupTipo`, `comissaoValor`, `comissaoTipo`

`preco` e `impostos` continuam visíveis. No histórico, a linha da alteração continua aparecendo, mas `valorAnterior` e `valorNovo` vêm `null` quando o campo alterado é sensível.

### Códigos de status

| Status | Quando acontece |
|--------|-----------------|
| `200` | Sucesso |
| `400` | Validação falhou (formato, precisão decimal, valor fora da lista de tipos) |
| `403` | Usuário sem a permissão exigida pelo endpoint |
| `404` | Item não encontrado |
| `401` | Token ausente ou inválido (fora do profile `local`) |
| `409` | Conflito de versão (o registro foi alterado por outra requisição) |
| `422` | Valor e tipo incompletos, ou valor `MONETARIO` com mais de 2 casas (o campo vem em `errors[].campo`) |
| `500` | Erro interno |

### Exemplo de uso (JavaScript)

```js
const BASE_URL = "http://localhost:8084"; // ou http://localhost:8080 pelo gateway

// Consultar
const res = await fetch(`${BASE_URL}/api/produtos/itens/${itemId}/precificacao`);
const { success, data, errors } = await res.json();

// Salvar
await fetch(`${BASE_URL}/api/produtos/itens/${itemId}/precificacao`, {
  method: "PUT",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ moeda: "BRL", custo: 100.0, preco: 150.0 }),
});
```

> Fora do profile `local`, as requisições precisam do header `Authorization: Bearer <token>` (o mesmo token do catálogo vale aqui).