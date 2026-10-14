# DevOps — backend do catálogo

## Arquivos

| Arquivo | Função |
|---|---|
| `Dockerfile` | Imagem da API: build Maven (Java 21) + runtime JRE Alpine, usuário sem privilégios, camadas do Spring Boot e `HEALTHCHECK` em `/api/produtos/health` |
| `.dockerignore` | Mantém `target/`, `.env`, `rodar-local.ps1` e documentação fora da imagem |
| `compose.yaml` | PostgreSQL 16 (`db`) e API (`api`, perfil `app`) |
| `docker/postgres/init/01-usuario-aplicacao.sh` | Na primeira subida do volume, cria o usuário `own_produtos`, dono do schema `produtos` |
| `.env.example` | Modelo das variáveis. Copie para `.env`, que não é versionado |
| `../.github/workflows/backend.yml` | CI: testes (H2), testes + migrations (PostgreSQL), build e smoke test da imagem, publicação no GHCR |
| `../.github/dependabot.yml` | Atualizações semanais de Maven, imagens Docker e Actions |

## Uso local

Pré-requisito: Docker Desktop em execução. Comandos dentro de `backend/`.

```powershell
Copy-Item .env.example .env            # uma vez; ajuste senhas e JWT
docker compose up -d                   # só o PostgreSQL, em localhost:5434
docker compose --profile app up -d --build   # PostgreSQL + API em localhost:8083
docker compose --profile app logs -f api
docker compose --profile app down      # para tudo (os dados ficam no volume)
docker compose --profile app down -v   # para tudo e APAGA os dados
```

Para usar o banco do container com o backend rodando fora do Docker (`rodar-local.ps1`), aponte
`DB_URL` para `jdbc:postgresql://localhost:5434/integrador`, com `DB_USER=own_produtos` e `DB_PASSWORD` do `.env`.

### Autenticação no container

A API do container usa a validação JWT real (`JWT_ISSUER`, `JWT_JWKS_URI`, `JWT_AUDIENCE`), configurada no `.env`.
O perfil `local`, com o token de desenvolvimento, **não funciona dentro do Docker**: ele escuta só em `127.0.0.1`
e emite token apenas para conexões do próprio computador. Pela rede do Docker, a conexão chega de outro IP.
Para desenvolver com o token local, rode o backend fora do Docker (`.\rodar-local.ps1`).

## CI (GitHub Actions)

Roda em push na `main` e em pull requests que alterem `backend/`:

1. **Testes (H2)**: `mvn verify`.
2. **Testes + migrations (PostgreSQL 16)**: aplica as migrations Flyway reais e valida o mapeamento JPA (`ddl-auto=validate`).
3. **Imagem Docker**: build, subida via compose e verificação do health.

Para publicar a imagem em `ghcr.io/<dono-do-repo>/produtos-api`, crie uma tag:

```powershell
git tag backend-v1.0.0
git push origin backend-v1.0.0
```

Isso gera as tags `1.0.0` e `latest`.

## Observações

- O PostgreSQL do container é a versão 16 porque o Flyway 10.10 do projeto suporta oficialmente até essa versão. Com o 17, ele só emite um aviso.
- Para os testes em PostgreSQL, exclua o `AutenticacaoLocalTest` (`-Dtest=!AutenticacaoLocalTest`). Ele fixa um H2 próprio e falha com `spring.flyway.enabled=true`. Isso também vale para o comando de testes em PostgreSQL do `README.md`.
