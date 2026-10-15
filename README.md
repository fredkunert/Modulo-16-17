# Módulo 16-17 — Produtos

Dois serviços Spring Boot que compartilham o mesmo banco PostgreSQL (`integrador`, schema `produtos`):

| Serviço | Pasta | Porta | Função |
|---|---|---|---|
| Catálogo | [`backend/`](backend/README.md) | 8083 | itens, fabricantes, distribuidores, busca; **dono das migrations** |
| Precificação | [`backend/backendprecificacao/`](backend/backendprecificacao/README.md) | 8084 | precificação manual e histórico de alterações |

Os dois usam o prefixo `/api/produtos`. Rotas `/api/produtos/itens/{id}/precificacao[...]` são do serviço de precificação;
todo o resto é do catálogo.

## Desenvolvimento com token falso (profile `local`)

1. `docker compose -f docker-compose.dev.yml up -d db`
2. Suba o catálogo e depois a precificação pelo IntelliJ/Maven com o profile `local` e
   `DB_URL=jdbc:postgresql://localhost:5432/integrador`, `DB_USER=own_produtos`, `DB_PASSWORD=produtos_dev`
   (detalhes em `backend/TESTE-LOCAL.md` e `backend/backendprecificacao/README.md`).
3. O front fala com `http://localhost:8083` (catálogo) e `http://localhost:8084` (precificação).

## Imagens Docker (modo produção, JWT real)

```bash
docker compose -f docker-compose.dev.yml up --build    # banco + catálogo + precificação + gateway
```

O gateway (`http://localhost:8080`) junta os dois serviços numa porta só. Em modo produção os serviços validam o JWT no
serviço de identidade (`JWT_JWKS_URI`); sem ele no ar, as rotas protegidas respondem 401. Variáveis em `.env.example`.
