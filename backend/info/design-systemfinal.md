# Design System — Centinela Soluções

> **Este documento é lei para todos os módulos do sistema.**
> Grupos de desenvolvimento diferentes, módulos diferentes — mesmo padrão visual e de
> interação. Se estiver construindo Financeiro, Contratos, Suporte ou qualquer outro módulo,
> siga este guia do início ao fim.

---

## Regra de ouro de UX (Regra 104)

Antes de criar qualquer interação, pergunte:

> *"O usuário precisa sair desta tela para executar esta ação?"*

- **Se NÃO** → ação contextual: modal, drawer, painel inline, menu rápido, edição inline.
- **Se SIM** → nova rota é justificada. Documente o motivo no PR.

Priorize sempre: **modais · drawers · menus rápidos · painéis laterais · ações inline · atalhos.**
Nunca navegue para outra página se a ação puder acontecer no contexto atual.

---

## 1. Stack técnica

| Camada | Tecnologia |
|---|---|
| **Frontend** | React 19 + TypeScript 5.7 + Vite 8 |
| **Estilização** | Tailwind CSS v4 (`@tailwindcss/vite`) — sem `tailwind.config.js` |
| **Ícones** | `lucide-react` — única fonte de ícones. Nunca emoji, nunca SVG inline avulso |
| **Backend** | Java Spring Boot (REST) |
| **Serviços** | MSM · Camada de serviço desacoplada |
| **Infraestrutura** | Docker |
| **Fonte** | Inter (Google Fonts, pesos 300–700) |

---

## 2. Tokens de cor — paleta Centinela

Definidos em `src/index.css` dentro do bloco `@theme`.
**Use sempre as classes Tailwind geradas. Nunca hex hardcoded em componentes.**

```css
/* src/index.css — alterar aqui muda o sistema inteiro */
@theme {
  --color-brand-950: #0C1A2E;  /* Navy escuro — sidebar, headers de página */
  --color-brand-900: #0F2340;  /* Navy médio */
  --color-brand-800: #0B6B4E;  /* Verde-teal escuro — botão primário, item ativo */
  --color-brand-700: #0D9B6E;  /* Verde-teal — hover, accent */
  --color-brand-600: #10B981;  /* Verde médio */
  --color-brand-500: #22C55E;  /* Verde vivo — logo accent */
  --color-brand-400: #4ADE80;  /* Verde claro — texto sobre fundo escuro */
  --color-brand-300: #86EFAC;  /* Verde mais claro — subtítulos no header */
  --color-brand-200: #BBF7D0;  /* Verde pastel */
  --color-brand-100: #DCFCE7;  /* Verde suave — backgrounds secundários */
  --color-brand-50:  #F0FDF4;  /* Verde mínimo — hover states */
}
```

| Intenção | Classe Tailwind |
|---|---|
| Fundo sidebar / header de página | `bg-brand-950` |
| Botão primário | `bg-brand-800 hover:bg-brand-700` |
| Item de nav ativo | `bg-brand-700` |
| Texto sobre fundo escuro (destaque) | `text-brand-400` |
| Texto sobre fundo escuro (caption) | `text-brand-300` |
| Background de hover / seção suave | `bg-brand-50` |
| Fundo de card / tag secundária | `bg-brand-100` |

---

## 3. Componentes disponíveis

Todos em `src/components/ui/index.tsx`. Importe via:

```tsx
import { Button, Modal, Input, Select, Badge, FormSection, FormField } from "@/components/ui";
```

| Componente | Responsabilidade |
|---|---|
| `Button` | Toda ação clicável com estilo |
| `Modal` | Todo overlay / dialog |
| `Input` | Campos de texto de linha única |
| `Select` | Campos de seleção nativa estilizada |
| `Textarea` | Campos de texto multilinha |
| `Badge` | Status, contadores, chips de categoria |
| `statusBadge(str)` | Retorna `<Badge>` com variant mapeado automaticamente |
| `FormSection` | Agrupa campos de formulário com número e título |
| `FormField` | Wrapper de campo com label e suporte a `colSpan` |
| `SearchBar` | Campo de busca com ícone embutido |
| `ViewToggle` | Alternador lista ↔ cards |
| `Pagination` | Paginação de listas |
| `FilterBar` | Container horizontal de filtros |
| `PageHeader` | Cabeçalho de página (fundo `brand-950`) |
| `DetailField` | Par label + valor para painéis de detalhe |
| `DetailSection` | Agrupa `DetailField` com número e título |
| `Stepper` | Indicador de progresso de wizard multi-step |

**Antes de criar um novo componente UI:** verifique se já existe na lista acima.
Se não existir, adicione em `src/components/ui/index.tsx` e documente aqui.

---

## 4. Button — variantes

```tsx
<Button variant="primary">Salvar</Button>      // ação principal
<Button variant="secondary">Exportar</Button>  // ação secundária
<Button variant="outline">Cancelar</Button>    // cancelar / ação neutra
<Button variant="ghost" size="sm">...</Button> // discreto / ícone
<Button variant="danger">Excluir</Button>      // destrutivo
```

| Situação | Variant |
|---|---|
| Criar / salvar / confirmar | `primary` |
| Ação secundária (importar, exportar, filtrar) | `secondary` |
| Cancelar / fechar / voltar | `outline` |
| Botão de ícone sutil | `ghost` + `size="sm"` |
| Deletar / rejeitar / ação irreversível | `danger` |

---

## 5. Modal — dois padrões

### 5.1 Modal com título (formulários e confirmações)

```tsx
<Modal
  open={aberto}
  onClose={() => setAberto(false)}
  title="Novo Registro"
  subtitle="Preencha os campos obrigatórios"
  size="xl"
  footer={
    <>
      <Button variant="outline" onClick={() => setAberto(false)}>Cancelar</Button>
      <Button onClick={salvar}>Salvar</Button>
    </>
  }
>
  {/* conteúdo */}
</Modal>
```

Tamanhos: `sm` (448px) · `md` (576px) · `lg` (672px) · `xl` (896px, padrão para formulários) · `full` (1152px)

### 5.2 Modal de painel (`noPad`) — detalhe de entidade

Use quando o conteúdo já tem seu próprio header colorido (brand-950 ou variante).
O modal não renderiza título próprio. Clicar no backdrop fecha.

```tsx
<Modal open={!!selecionado} onClose={() => setSelecionado(null)} size="xl" noPad>
  {selecionado && (
    <EntidadeDetailPanel entidade={selecionado} onClose={() => setSelecionado(null)} />
  )}
</Modal>
```

**Regra:**
- Detalhe de entidade (qualquer módulo) → `Modal noPad`
- Formulário de criação/edição → `Modal` com `title`
- Confirmação de ação → `Modal size="sm"` com `title`

---

## 6. Formulários — FormSection + FormField

```tsx
<FormSection number={1} title="Dados Principais">
  <FormField label="Nome" required colSpan={2}>
    <Input value={form.nome} onChange={(e) => upd("nome", e.target.value)} />
  </FormField>
  <FormField label="Tipo">
    <Select value={form.tipo} onChange={(e) => upd("tipo", e.target.value)}>
      <option value="">Selecione…</option>
      <option>Opção A</option>
    </Select>
  </FormField>
  <FormField label="Data">
    <Input type="date" value={form.data} onChange={(e) => upd("data", e.target.value)} />
  </FormField>
</FormSection>
```

- `FormSection` cria grid de **2 colunas** internamente
- `colSpan={2}` para campos que devem ocupar a linha inteira
- Campos condicionais **sempre por último** no `FormSection` (evitar buracos no grid)
- Em mobile o grid colapsa para 1 coluna automaticamente

---

## 7. Badge — status de entidades

```tsx
<Badge variant="green">ativo</Badge>
<Badge variant="red">cancelado</Badge>
<Badge variant="blue">em andamento</Badge>
<Badge variant="indigo">pendente</Badge>
<Badge variant="yellow">rascunho</Badge>
<Badge variant="gray">sem resposta</Badge>
<Badge variant="purple">atenção</Badge>

// Mapeamento automático (CRM):
{statusBadge(entidade.status)}
```

Cada módulo pode estender o mapa de `statusBadge` para seus próprios status — mantendo as
variantes de cor existentes. Não crie novas cores de badge sem adicionar ao design system.

---

## 8. Ícones — lucide-react

Única fonte de ícones do sistema. Nunca emoji, nunca SVG inline avulso sem propósito estrutural.

```tsx
import { Building2, FileText, Settings, Phone } from "lucide-react";

<Phone className="w-4 h-4" />               // tamanho padrão
<Building2 className="w-4 h-4 shrink-0" />  // em flex containers de lista
```

| Tamanho | Uso |
|---|---|
| `w-3 h-3` | Dentro de botões `sm`, chips, badges |
| `w-4 h-4` | Uso geral — tabelas, listas, inline de texto |
| `w-5 h-5` | Ações rápidas, menus de ação |
| `w-8 h-8`+ | Estado vazio, modais de confirmação, ilustrações |

Sempre use `shrink-0` em ícones dentro de containers flex de lista para evitar compressão.

---

## 9. Navegação — sidebar e módulos

A sidebar (`src/App.tsx`) é o ponto central de navegação. Cada grupo de desenvolvimento
**adiciona seu módulo** registrando no array `NAV` e em `pageMap`.

```tsx
// src/App.tsx — como adicionar um módulo novo
const NAV: NavItem[] = [
  // módulos existentes...
  { id: "financeiro", label: "Financeiro", icon: <DollarSign className="w-4 h-4 shrink-0" />, group: "Financeiro" },
];

const pageMap: Record<Page, React.ReactElement> = {
  // módulos existentes...
  financeiro: <FinanceiroPage />,
};

---

## 10. Border Radius — Padrões de Arredondamento

Para garantir consistência em cartões, modais, botões e campos de entrada, utilizamos a escala padrão de arredondamento do Tailwind CSS. 
**Nunca utilize valores arbitrários de border-radius (ex.: `rounded-[7px]`) nos componentes.**

| Elemento / Componente | Classe Tailwind | Valor / Aplicação |
|---|---|---|
| **Botões, Inputs, Selects, Badges** | `rounded-lg` | Arredondamento padrão para elementos interativos menores |
| **Cards, Modais, Containers de seção** | `rounded-xl` | Blocos de conteúdo, painéis e janelas de diálogo |
| **Elementos circulares (Avatares, ícones de perfil)** | `rounded-full` | Imagens de perfil, ícones de status redondos |
| **Badges de contagem ou pílulas compactas** | `rounded-full` | Etiquetas e tags arredondadas |

### Diretrizes de Uso:
- **Modais e Drawers:** Devem utilizar consistentemente `rounded-xl` em suas bordas externas.
- **Elementos Aninhados:** Certifique-se de que elementos internos respeitem o raio do container pai para evitar distorções visuais (regra do *overflow-hidden* em containers principais).

```

**Regras da sidebar:**
- O `group` agrupa os itens com um separador de seção
- Use um grupo por domínio (ex.: `"CRM"`, `"Financeiro"`, `"Suporte"`)
- O ícone deve ser da biblioteca `lucide-react`
- A rota é o id da página — use nomes descritivos em camelCase

**O que NÃO fazer:**
- Não criar um segundo `App.tsx` ou um segundo ponto de entrada
- Não criar rotas novas para ações que podem ser modal dentro do módulo
- Não quebrar o layout da sidebar ou o header global da aplicação

---

## 11. Estrutura de módulo — padrão por grupo

Cada módulo vive em `src/features/<nome-do-modulo>/`. Estrutura mínima:

```
src/features/financeiro/
├── FinanceiroPage.tsx      ← componente principal, registrado no App.tsx
├── components/
│   ├── FinanceiroList.tsx
│   ├── FinanceiroDetailPanel.tsx
│   └── FinanceiroForm.tsx
└── (tipos específicos podem ficar em src/data/mock.ts ou src/types/)
```

O componente principal (`FinanceiroPage.tsx`) é o único registrado no `pageMap`.
Os sub-componentes ficam na pasta `components/` do módulo.

---

## 12. Padrões de layout — aplicáveis a qualquer módulo

### 12.1 Lista com detalhe em modal

O padrão mais comum do sistema. Aplicar em qualquer módulo que liste entidades.

```
┌─────────────────────────────────────────┐
│  Header brand-950 (título + CTAs)       │
├─────────────────────────────────────────┤
│  FilterBar (search + selects de filtro) │
├─────────────────────────────────────────┤
│  Tabela ou Grid de Cards  (flex-1)      │
│   └─ clique → Modal noPad com painel   │
├─────────────────────────────────────────┤
│  Pagination                             │
└─────────────────────────────────────────┘
```

- A lista ocupa sempre `flex-1` — largura total da tela
- O detalhe abre em `Modal size="xl" noPad`
- Edição abre em `Modal size="xl"` com `title`

### 12.2 Workspace com painel lateral (Kanban, dashboards interativos)

```
┌─────────────────────────────────────────┐
│  Header brand-950 (métricas + ações)    │
├─────────────────────────────────────────┤
│  Área principal (kanban / gráfico / ...) │
│   └─ clique em item → Modal noPad      │
│         com workspace completo          │
└─────────────────────────────────────────┘
```

### 12.3 Cabeçalho de página — padrão obrigatório

Todo módulo usa este padrão de header:

```tsx
<div className="flex items-center justify-between px-5 py-3.5 bg-brand-950 text-white shrink-0">
  <div>
    <h1 className="text-lg font-bold tracking-tight">Nome do Módulo</h1>
    <p className="text-brand-300 text-xs mt-0.5">{n} registros encontrados</p>
  </div>
  <div className="flex gap-2">
    <Button variant="secondary" size="sm">Ação secundária</Button>
    <Button size="sm">Ação principal</Button>
  </div>
</div>
```

### 12.4 Painel de detalhe de entidade — padrão de header

Painéis abertos via `Modal noPad` têm seu próprio header interno:

```tsx
<div className="flex flex-col overflow-hidden bg-white w-full">
  {/* Header colorido — usa bg-brand-950 como padrão */}
  <div className="px-5 pt-5 pb-3 bg-brand-950 text-white shrink-0">
    <div className="flex items-start justify-between mb-3">
      <div>
        <h2 className="font-bold text-lg">{entidade.nome}</h2>
        <p className="text-brand-300 text-sm">{entidade.subtitulo}</p>
      </div>
      <div className="flex gap-1.5">
        <Button variant="secondary" size="sm" onClick={onEdit}>
          <Pencil className="w-3 h-3" /> Editar
        </Button>
        <button onClick={onClose} className="p-1.5 rounded-lg hover:bg-brand-800 text-brand-300 hover:text-white">
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  </div>

  {/* Tabs de navegação interna */}
  <div className="flex border-b border-gray-100 bg-white shrink-0 overflow-x-auto">
    {TABS.map((t, i) => (
      <button key={t} onClick={() => setTab(i)}
        className={`px-4 py-2.5 text-xs font-semibold whitespace-nowrap border-b-2 transition-colors
          ${tab === i ? "border-brand-700 text-brand-800" : "border-transparent text-gray-500 hover:text-gray-700"}`}>
        {t}
      </button>
    ))}
  </div>

  {/* Conteúdo das tabs */}
  <div className="flex-1 overflow-y-auto p-5">
    {/* conteúdo */}
  </div>
</div>
```

---

## 13. Regras de `style={{}}` inline

`style={{}}` é aceitável **somente** para valores calculados em runtime:

```tsx
// ✅ Correto — cor vinda de dado (ex.: cor configurável de etapa)
<div style={{ background: etapa.color }} />

// ✅ Correto — largura de barra de progresso calculada
<div style={{ width: `${progresso}%` }} />

// ✅ Correto — cor de item ativo na sidebar (hover inline)
style={ativo ? { background: "#0D9B6E" } : {}}

// ❌ Errado — estilização estática que deve ser classe Tailwind
<div style={{ padding: "16px", fontWeight: "bold" }} />

// ❌ Errado — hex hardcoded sem ser valor dinâmico
<div style={{ background: "#0C1A2E" }} />  // use bg-brand-950
```

---

## 14. Comunicação com o backend (Spring Boot / REST)

- Chamadas HTTP via `fetch` ou `axios` com base URL de variável de ambiente (`import.meta.env.VITE_API_URL`)
- Tipos TypeScript para entidades em `src/data/mock.ts` (prototipagem) → `src/types/<modulo>.ts` (produção)
- Dados mockados são substituídos por `useEffect + fetch` sem alterar estrutura de componentes
- Erros de API retornam feedback visual — nunca `alert()` ou `console.error` silencioso
- Loading states usam skeleton ou spinner dentro do container do conteúdo, nunca bloqueiam a sidebar/header

---

## 15. O que NÃO fazer — anti-padrões

| Anti-padrão | Correto |
|---|---|
| Criar nova página para ação que cabe em modal | `Modal` com formulário ou `Modal noPad` com painel |
| Estilizar `<button>`, `<input>`, `<select>` nativos na mão | Usar `Button`, `Input`, `Select` de `@/components/ui` |
| Hex hardcoded (`#0C1A2E`, `#0D9B6E`) em componentes | Classes `bg-brand-950`, `bg-brand-700` |
| Usar emoji como ícone de UI | `lucide-react` |
| Criar um segundo `App.tsx` ou shell paralelo | Registrar o módulo no `NAV` e `pageMap` do `App.tsx` existente |
| Criar rota nova para cada sub-entidade | Tabs internas no painel de detalhe |
| `style={{}}` para estilização estática | Classe Tailwind |
| Abrir detalhes em página nova | `Modal noPad` com painel de detalhe |
| Ignorar mobile | Grid `grid-cols-1 md:grid-cols-2` nos formulários |

---

## 16. Checklist de PR com UI nova

- [ ] Componentes de `@/components/ui` — sem HTML nativo estilizado à mão
- [ ] Tokens de cor (`bg-brand-*`, `text-brand-*`) — sem hex hardcoded
- [ ] Ícones via `lucide-react` — sem emoji, sem SVG avulso
- [ ] Ação contextual sempre que possível (Regra 104) — sem nova rota desnecessária
- [ ] `style={{}}` apenas para valores dinâmicos de runtime
- [ ] Formulários com `FormSection` + `FormField` + labels explícitos
- [ ] Modal de detalhe: `noPad`. Modal de formulário: com `title`. Confirmação: `size="sm"`
- [ ] Campos condicionais no final do `FormSection`
- [ ] `shrink-0` em ícones dentro de flex containers de lista
- [ ] Módulo registrado em `NAV` e `pageMap` em `src/App.tsx`
- [ ] Estrutura de arquivos em `src/features/<modulo>/`
- [ ] `npx tsc --noEmit` sem erros

---

## 17. Referências no código (módulo CRM — referência canônica)

| Padrão | Arquivo de referência |
|---|---|
| Tokens de cor e fonte | `src/index.css` |
| Todos os componentes base | `src/components/ui/index.tsx` |
| Shell + sidebar + search global | `src/App.tsx` |
| Lista + modal de detalhe | `src/features/companies/CompaniesPage.tsx` |
| Formulário completo multi-seção | `src/features/companies/CompaniesPage.tsx` → `CompanyForm` |
| Painel de detalhe com tabs | `src/features/companies/CompaniesPage.tsx` → `CompanyDetailPanel` |
| Kanban + workspace modal | `src/features/pipeline/PipelinePage.tsx` |
| Wizard multi-step com Stepper | `src/features/companies/CompaniesPage.tsx` → `ImportWizardModal` |
| Tipos de dados e mock | `src/data/mock.ts` |
