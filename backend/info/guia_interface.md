# Anatomia da Interface — Centinela CRM (Módulo Empresas)

Este documento detalha o mapeamento visual, técnico e estrutural da tela de listagem de empresas do módulo CRM, relacionando cada elemento visível com os tokens do Design System da **Centinela Soluções**.

---

## 1. Visão Geral do Layout & Grid

A interface segue estritamente o padrão de **Layout Flexbox com Sidebar Fixa e Header Global**, ocupando 100% da viewport sem rolagem horizontal.

```
┌─────────────────┬────────────────────────────────────────────────────────┐
│                 │ 1. Header Global (bg-brand-950)                        │
│                 ├────────────────────────────────────────────────────────┤
│ 2. Sidebar      │ 2. Page Header (bg-brand-950)                          │
│ (bg-brand-950)  ├────────────────────────────────────────────────────────┤
│                 │ 3. Filter Bar (Filtros e Busca)                        │
│                 ├────────────────────────────────────────────────────────┤
│                 │ 4. Tabela de Dados (flex-1, linhas com border-b)       │
│                 └────────────────────────────────────────────────────────┘
```

---

## 2. Mapeamento Detalhado por Seção

### 2.1. Sidebar (Menu Lateral)
- **Fundo:** `bg-brand-950` (`#0C1A2E`)
- **Logo / Marca:** Texto em fonte `Inter`, peso Bold, cor branca (`text-white`) com subtítulo verde claro (`text-brand-300`).
- **Seção Ativa ("Empresas"):** Fundo `bg-brand-700` (`#0D9B6E`) com ícone da biblioteca `lucide-react` (`Building2` / `w-4 h-4 shrink-0`).
- **Itens Inativos ("Contatos", "Funil de Vendas"):** Texto em cor cinza-claro, transição suave para `hover:bg-brand-800` (`#0B6B4E`).
- **Rodapé da Sidebar ("Recolher"):** Botão de ação discreto alinhado à esquerda com ícone de seta (`w-4 h-4 text-brand-300`).

---

### 2.2. Header Global (Superior Direito)
- **Fundo:** Integrado ao contexto da página ou `bg-brand-950`.
- **Barra de Pesquisa Global:** Componente `SearchBar` com ícone de busca embutido (`lucide-react`, `w-4 h-4`), borda sutil, atalho de teclado `⌘K` exibido à direita em um Badge cinza suave (`rounded-lg`, `text-xs`).
- **Notificações:** Ícone de sino com indicador numérico em Badge vermelho (`rounded-full`, tamanho compacto).
- **Avatar do Usuário ("AF"):** Componente circular (`rounded-full`) com fundo verde primário (`bg-brand-700`) e iniciais em texto branco (`text-white`, `font-semibold`).

---

### 2.3. Page Header (Cabeçalho do Módulo)
- **Fundo:** `bg-brand-950` (`#0C1A2E`), altura compacta com padding vertical (`py-3.5 px-5`).
- **Título da Página ("Empresas"):** Tipografia `Inter`, peso Bold (`font-bold`), tamanho `text-lg`, tracking ajustado (`tracking-tight`), cor branca (`text-white`).
- **Breadcrumb ("CRM / Empresas"):** Texto auxiliar acima do título em cor clara (`text-brand-300`, `text-xs`).
- **Contador de Registros ("6 empresas encontradas"):** Texto secundário em verde claro (`text-brand-300`, tamanho `text-xs`).
- **Ações Principais (Botões à Direita):**
  - *Importar:* Componente `Button` variante `secondary` (`bg-gray-100` ou similar sutil) com ícone `Upload` (`w-4 h-4`).
  - *Nova Empresa:* Componente `Button` variante `primary` (`bg-brand-800 hover:bg-brand-700`) com ícone de adição (`Plus`, `w-4 h-4`) e texto em branco.

---

### 2.4. Filter Bar (Barra de Filtros e Visualização)
- **Container:** Fundo branco com espaçamento interno uniforme (`p-4` ou `px-5 py-3`), borda inferior de separação (`border-b border-gray-100`).
- **Campo de Busca Interna:** Input de linha única (`Input`) com ícone de lupa à esquerda e placeholder descritivo (`"Buscar razão social, fantasia ou CNPJ..."`), largura estendida (`flex-1` ou tamanho fixo grande).
- **Selects de Filtro ("Segmento", "Status", "Porte"):** Componentes `Select` nativos estilizados com setas indicativas à direita e bordas padrão (`rounded-lg`, `border-gray-300`).
- **Alternador de Visualização ("Lista" vs "Cards"):** Componente `ViewToggle` com botões segmentados. O botão ativo ("Lista") recebe destaque em tom escuro/primário (`bg-brand-950 text-white`), enquanto o inativo permanece neutro (`hover:bg-gray-50`).

---

### 2.5. Tabela de Dados (Listagem Principal)
- **Estrutura:** Cabeçalho de tabela em caixa alta, texto cinza escuro (`text-gray-500` / `font-semibold`, `text-xs`), com divisórias limpas (`border-b`).
- **Linhas da Tabela:** Cada linha possui espaçamento interno confortável (`py-3`), efeito de hover suave (`hover:bg-brand-50` ou `hover:bg-gray-50/50`), e separador inferior (`border-b border-gray-100`).
- **Avatar / Ícone da Empresa (Coluna 1):** Badge circular escura (`w-8 h-8 rounded-full bg-brand-950 text-brand-400 font-bold flex items-center justify-center text-xs`) contendo as iniciais da empresa (ex: `HO`, `TE`, `AL`).
- **Nome e Localização:** Nome principal em azul escuro/negrito (`text-brand-950` ou azul corporativo, `font-medium`), seguido pela cidade/estado em texto menor e cinza (`text-gray-400`, `text-xs`).
- **Dados Textuais (CNPJ, Segmento, Vendedor):** Tipografia `Inter` Regular (`text-sm`), cor de texto principal cinza-escuro (`text-gray-700`).

---

### 2.6. Badges de Status (Colunas Comerciais, Financeiras, etc.)
O sistema utiliza o componente utilitário `statusBadge(str)` mapeado para renderizar o componente `Badge` com variantes de cor específicas:
- **Ativo / Adimplente / Concluído:** Variante `green` (`bg-brand-100 text-brand-800` ou tons esmeralda).
- **Prospect / Lead / Em Andamento:** Variante `blue` / `indigo` (`bg-blue-50 text-blue-700` ou similar).
- **Em Elaboração / Rascunho / Amarelo:** Variante `yellow` (`bg-yellow-50 text-yellow-800`).
- **Inativo / Cancelado / Inadimplente:** Variante `red` (`bg-red-50 text-red-700`).
- **Suspenso / Atenção / Laranja:** Variante `orange` / `purple` para estados de alerta.

---

## 3. Resumo de Tokens Aplicados nesta Tela

| Categoria | Token / Valor Utilizado | Elemento Associado |
|---|---|---|
| **Cores Primárias** | `bg-brand-950` (`#0C1A2E`) | Sidebar, Headers Principais |
| **Cores de Destaque** | `bg-brand-700` (`#0D9B6E`) | Item ativo da sidebar, botões de ação |
| **Tipografia** | `Inter` (Pesos 300 a 700) | Toda a interface do sistema |
| **Border Radius** | `rounded-lg` / `rounded-xl` / `rounded-full` | Inputs, botões, modais e avatares |
| **Ícones** | `lucide-react` (tamanhos `w-4 h-4`, `w-5 h-5`) | Navegação, busca, ações rápidas |