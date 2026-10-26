# Frontend — Catálogo de Produtos (Centinela)

Interface do módulo **Produtos** (itens, fabricantes e distribuidores) para o backend em [`../backend`](../backend), seguindo o design system em [`../backend/info`](../backend/info).

Stack: React 19 · TypeScript · Vite 8 · Tailwind CSS v4 (`@tailwindcss/vite`) · `lucide-react`.

## Executar

1. Inicie o backend no perfil `local` (veja [`TESTE-LOCAL.md`](../backend/TESTE-LOCAL.md)) — porta **8083**.
   Para a aba **Precificação**, inicie também o `backendprecificacao` no perfil `local` — porta **8084** (veja [`ENDPOINTS-PRECIFICACAO.md`](../backend/backendprecificacao/ENDPOINTS-PRECIFICACAO.md)).
2. No diretório `frontend`:

```powershell
npm install
npm run dev
```

Abra http://localhost:5173. No perfil `local`, o frontend obtém sozinho um token em `POST /api/produtos/dev/token` e o renova após um 401.
Sem o perfil `local`, use **Usar token da equipe** e cole um JWT válido.

Em desenvolvimento, as chamadas a `/api` passam pelo proxy do Vite (`vite.config.ts`): `/api/produtos/itens/{id}/precificacao…` vai para `http://127.0.0.1:8084` (altere com `PRECIFICACAO_URL` no `.env`) e o resto para `http://127.0.0.1:8083` (altere com `BACKEND_URL`) — a mesma divisão do `gateway/nginx.conf`. Em produção, sirva o `dist/` atrás do mesmo domínio da API ou defina `VITE_API_URL`.

## Telas

| Tela | Rotas usadas |
|---|---|
| **Itens**: lista/cards, filtros (busca, categoria, fabricante, distribuidor, situação), ordenação e paginação | `GET /itens` |
| Detalhe do item (modal `noPad` com abas: visão geral, precificação, registro) | `GET /itens/{id}` |
| Novo/editar item (modal com `FormSection`). A edição envia via PATCH **somente os campos alterados** | `POST /itens`, `PATCH /itens/{id}` |
| Inativar/reativar (modal de confirmação `sm`) | `PATCH /itens/{id}` `{"ativo":…}` |
| **Fabricantes** / **Distribuidores**: lista, cadastro, edição, inativação e itens vinculados | `GET/POST/PATCH /fabricantes`, `/distribuidores` |
| Busca global (Ctrl/⌘ + K) | `GET /busca?q=` |
| Notificações: status do backend e validade do token | `GET /health` |

As permissões do token (`produtos.item.criar`, `editar`, `ver`, `ver_resumo`) controlam quais botões aparecem. O perfil `resumo` acessa somente a busca global. Pelo menu do avatar é possível alternar entre os perfis.

## Estrutura

```
src/
├── App.tsx                    ← shell: sidebar (NAV + pageMap), header global
├── index.css                  ← tokens @theme da paleta Centinela
├── components/ui/index.tsx    ← componentes do design system (+ Toast, Spinner, EmptyState, Avatar)
├── components/shell/          ← busca global, notificações, menu do usuário, tela de sessão
├── features/itens/            ← ItensPage + components/ (ItemForm, ItemDetailPanel, ItemList)
├── features/parceiros/        ← página compartilhada de fabricantes/distribuidores
├── features/fabricantes/, features/distribuidores/
├── lib/                       ← api (fetch + envelope ApiResponse), sessão/JWT, hooks, formatação
└── types/produtos.ts          ← contratos espelhados dos DTOs Java
```

Verificação: `npm run typecheck` e `npm run build`.
