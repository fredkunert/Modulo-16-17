# Módulo 16-17 — Produtos

Sistema de catálogo e precificação de produtos da Centinela. O projeto é composto
por uma interface React, um serviço Spring Boot e um gateway Nginx. O backend
compartilha o banco PostgreSQL `integrador`, no schema `produtos`.

## Arquitetura

| Componente | Diretório | Porta direta | Responsabilidade |
|---|---|---:|---|
| Frontend | [`frontend/`](frontend/) | 5173 | Interface web do catálogo |
| Catálogo | [`backend/`](backend/) | 8083 | Itens, fabricantes, distribuidores e busca |
| Precificação | [`backend/src/main/java/br/edu/pi2026/produtos/precificacao/`](backend/src/main/java/br/edu/pi2026/produtos/precificacao/) | 8083 | Preços, margens e histórico |
| Gateway | [`gateway/nginx.conf`](gateway/nginx.conf) | 8080 | Une as duas APIs em uma porta |
| PostgreSQL | Docker Compose | 5432 | Banco compartilhado |

O serviço de catálogo é o dono das migrations Flyway (`V1`, `V2` e `V3`).
O serviço de precificação não executa migrations.

## Pré-requisitos

- Java 21
- Maven 3.9+
- Node.js e npm
- Docker Desktop com Docker Compose
- PostgreSQL, caso os serviços sejam executados fora do Docker

## Executar com Docker

Este é o modo mais simples para subir banco, APIs e gateway:

```powershell
docker compose -f docker-compose.dev.yml up --build
```

Após a inicialização:

- Frontend: execute-o separadamente e abra <http://localhost:5173>
- Gateway/API: <http://localhost:8080>
- Catálogo: <http://localhost:8083>
- Precificação: <http://localhost:8083>
- Banco: `localhost:5432`, database `integrador`, usuário `own_produtos`

Para executar somente o banco:

```powershell
docker compose -f docker-compose.dev.yml up -d db
```

O Compose usa `DB_PASSWORD=produtos_dev` por padrão. Para alterar a senha,
defina `DB_PASSWORD` antes de iniciar os serviços. As APIs em Docker executam
sem o profile `local` e, portanto, precisam de um provedor JWT acessível pelos
valores `JWT_ISSUER`, `JWT_JWKS_URI` e `JWT_AUDIENCE`.

## Executar localmente para desenvolvimento

O profile `local` gera um JWT de desenvolvimento para o backend. Ele deve ser
usado somente na máquina do desenvolvedor.

### 1. Subir o banco

```powershell
docker compose -f docker-compose.dev.yml up -d db
```

### 2. Subir o backend

Em um terminal, na pasta [`backend/`](backend/):

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/integrador'
$env:DB_USER = 'own_produtos'
$env:DB_PASSWORD = 'produtos_dev'
mvn spring-boot:run '-Dspring-boot.run.profiles=local'
```

O backend executa as migrations e fica disponível em
`http://127.0.0.1:8083`.

### 3. Subir o frontend

Em outro terminal, na pasta [`frontend/`](frontend/):

```powershell
npm install
npm run dev
```

Abra <http://localhost:5173>. O Vite encaminha automaticamente:

- todas as rotas `/api` para a porta 8083.

A URL pode ser alterada com `BACKEND_URL` no `.env` do frontend. Para produção, use `VITE_API_URL` conforme
[`frontend/README.md`](frontend/README.md).

## Autenticação local

Com o catálogo no profile `local`, obtenha um token:

```powershell
$login = Invoke-RestMethod -Method Post `
  -Uri 'http://127.0.0.1:8083/api/produtos/dev/token'
$token = $login.data.accessToken
$headers = @{ Authorization = "Bearer $token" }
```

O token padrão representa o usuário `usuario-local`, no tenant
`tenant-local`, com permissões completas de catálogo. O endpoint de token
existe somente no profile `local` e não deve ser publicado.

O roteiro completo, incluindo perfis com permissões reduzidas e isolamento por
tenant, está em [`docs/TESTE-LOCAL.md`](docs/TESTE-LOCAL.md).

## Endpoints

Todas as rotas usam o prefixo `/api/produtos`. Em desenvolvimento, elas podem
ser acessadas diretamente na porta do serviço ou pelo gateway em
`http://localhost:8080`.

### Saúde

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `GET` | `/health` | Pública | Verifica a aplicação e a conexão com o banco |

### Catálogo

Serviço padrão: porta 8083. Permissões usadas: `produtos.item.criar`,
`produtos.item.ver`, `produtos.item.editar` e `produtos.item.ver_resumo`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/itens` | Cadastra um item |
| `GET` | `/itens` | Lista itens com paginação, filtros e ordenação |
| `GET` | `/itens/{id}` | Consulta um item |
| `PATCH` | `/itens/{id}` | Altera somente os campos enviados |
| `GET` | `/itens/{id}/resumo` | Consulta o resumo de um item |
| `GET` | `/itens/resumo?ids={uuid1},{uuid2}` | Consulta vários resumos |
| `GET` | `/busca?q={termo}` | Busca global de itens ativos, até cinco resultados |
| `GET` | `/fabricantes` | Lista fabricantes |
| `GET` | `/fabricantes/{id}` | Consulta um fabricante |
| `POST` | `/fabricantes` | Cadastra um fabricante |
| `PATCH` | `/fabricantes/{id}` | Altera um fabricante |
| `GET` | `/distribuidores` | Lista distribuidores |
| `GET` | `/distribuidores/{id}` | Consulta um distribuidor |
| `POST` | `/distribuidores` | Cadastra um distribuidor |
| `PATCH` | `/distribuidores/{id}` | Altera um distribuidor |

As listagens aceitam `pagina`, `tamanho` e `ordenar=nome,asc` ou
`ordenar=nome,desc`. O tamanho máximo é 100. A listagem de itens também aceita
`q`, `categoria`, `fabricanteId`, `distribuidorId` e `ativo`.

Exemplo de criação:

```http
POST http://localhost:8083/api/produtos/itens
Authorization: Bearer <token>
Content-Type: application/json

{
  "sku": "BKP-001",
  "nome": "Licença de backup",
  "categoria": "BACKUP",
  "unidade": "USUARIO",
  "periodicidade": "MENSAL",
  "quantidadeMinima": "1.0000"
}
```

### Precificação

Serviço padrão: porta 8083. O `{itemId}` é o UUID de um item existente no
catálogo. Permissões: `produtos.precificacao.ver`,
`produtos.precificacao.editar` e, para valores sensíveis,
`produtos.precificacao.ver_valores_sensiveis`.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/itens/{itemId}/precificacao` | Consulta a precificação vigente |
| `PUT` | `/itens/{itemId}/precificacao` | Cria ou atualiza a precificação |
| `GET` | `/itens/{itemId}/precificacao/historico` | Lista o histórico paginado |

Exemplo de atualização:

```http
PUT http://localhost:8083/api/produtos/itens/{itemId}/precificacao
Content-Type: application/json

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

O contrato detalhado está em
[`backend/contratos/precificacao.yaml`](backend/contratos/precificacao.yaml),
e as regras completas estão em
[`docs/ENDPOINTS-PRECIFICACAO.md`](docs/ENDPOINTS-PRECIFICACAO.md).

### Envelope e códigos de resposta

As APIs retornam o envelope:

```json
{
  "success": true,
  "data": {},
  "message": null,
  "errors": []
}
```

Principais status: `200` para consultas e alterações, `201` para criações,
`400` para formato inválido, `401` para token ausente ou inválido, `403` para
permissão insuficiente, `404` para recurso inexistente, `409` para conflito e
`422` para regras de negócio.

## Documentação e testes manuais

- Catálogo: [`backend/README.md`](backend/README.md)
- Documentação operacional: [`docs/DEVOPS.md`](docs/DEVOPS.md)
- Teste local: [`docs/TESTE-LOCAL.md`](docs/TESTE-LOCAL.md)
- Requisições do catálogo: [`backend/exemplos.http`](backend/exemplos.http)
- Requisições de precificação: [`backend/requests-precificacao.http`](backend/requests-precificacao.http)
- Regras de precificação: [`docs/PRECIFICACAO.md`](docs/PRECIFICACAO.md)

## Testes e build

Backend do catálogo:

```powershell
cd backend
mvn test
mvn package
```

Frontend:

```powershell
cd frontend
npm run typecheck
npm run build
```

Os testes dos backends usam H2 em memória por padrão. Para testes de integração
com PostgreSQL, consulte [`backend/README.md`](backend/README.md) e use um banco
exclusivo e descartável.

## Configuração de produção

Fora do profile `local`, configure no backend:

```text
DB_URL=jdbc:postgresql://<host>:5432/integrador
DB_USER=<usuario>
DB_PASSWORD=<senha>
JWT_ISSUER=<issuer>
JWT_JWKS_URI=<url-do-jwks>
JWT_AUDIENCE=produtos
JWT_PERMISSIONS_CLAIM=permissions
```

Consulte [`backend/.env.example`](backend/.env.example) para a lista de
variáveis do backend. Nunca versione senhas, tokens ou chaves privadas.
