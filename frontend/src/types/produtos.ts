// Contratos espelhados de backend/src/main/java/ProjetoIntegrador/**/dto

export const CATEGORIAS = [
  "SOFTWARE", "LICENÇA", "SERVIÇO", "PROJETO", "CONSULTORIA", "BACKUP", "ARMAZENAMENTO", "CLOUD",
  "SEGURANÇA", "ANTIVÍRUS", "EDR", "XDR", "MICROSOFT 365", "GOOGLE WORKSPACE", "SUPORTE", "OUTROS",
] as const;
export type Categoria = (typeof CATEGORIAS)[number];

export const UNIDADES = [
  "USUARIO", "LICENCA", "ENDPOINT", "SERVIDOR", "VM", "GB", "TB", "CAIXA_POSTAL", "DOMINIO", "HORA", "UNIDADE",
] as const;
export type Unidade = (typeof UNIDADES)[number];

export const PERIODICIDADES = ["UNICA", "MENSAL", "TRIMESTRAL", "SEMESTRAL", "ANUAL"] as const;
export type Periodicidade = (typeof PERIODICIDADES)[number];

export const UNIDADE_LABEL: Record<Unidade, string> = {
  USUARIO: "Usuário", LICENCA: "Licença", ENDPOINT: "Endpoint", SERVIDOR: "Servidor", VM: "VM",
  GB: "GB", TB: "TB", CAIXA_POSTAL: "Caixa postal", DOMINIO: "Domínio", HORA: "Hora", UNIDADE: "Unidade",
};

export const PERIODICIDADE_LABEL: Record<Periodicidade, string> = {
  UNICA: "Única", MENSAL: "Mensal", TRIMESTRAL: "Trimestral", SEMESTRAL: "Semestral", ANUAL: "Anual",
};

/** GB, TB e HORA aceitam até 4 casas decimais; as demais unidades exigem inteiros. */
export const casasDecimais = (u: Unidade) => (u === "GB" || u === "TB" || u === "HORA" ? 4 : 0);

export interface Item {
  id: string;
  sku: string;
  nome: string;
  fabricanteId: string | null;
  distribuidorId: string | null;
  categoria: Categoria;
  descricao: string | null;
  unidade: Unidade;
  periodicidade: Periodicidade;
  quantidadeMinima: string;
  casasDecimaisQuantidade: number;
  ativo: boolean;
  moeda: string;
  precoVenda: number | null;
  precoMinimo: number | null;
  descontoMaximo: number | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ItemCriacao {
  sku: string;
  nome: string;
  fabricanteId: string | null;
  distribuidorId: string | null;
  categoria: Categoria;
  descricao: string | null;
  unidade: Unidade;
  periodicidade: Periodicidade;
  quantidadeMinima: string;
  ativo: boolean;
}

export type ItemAtualizacao = Partial<ItemCriacao>;

/** Fabricantes e distribuidores têm o mesmo contrato. */
export interface Parceiro {
  id: string;
  nome: string;
  ativo: boolean;
}

export interface BuscaResultado {
  id: string;
  titulo: string;
  subtitulo: string;
  rota: string;
}

// Contratos espelhados de backend/backendprecificacao/.../precificacao/dto

export type MarkupTipo = "PERCENTUAL" | "FATOR";
export type ValorTipo = "PERCENTUAL" | "MONETARIO";

/** Todo campo é opcional e independente: null significa "não informado", nunca zero. */
export interface PrecificacaoDados {
  moeda: string | null;
  custo: number | null;
  preco: number | null;
  margemPercentual: number | null;
  markupValor: number | null;
  markupTipo: MarkupTipo | null;
  impostosValor: number | null;
  impostosTipo: ValorTipo | null;
  comissaoValor: number | null;
  comissaoTipo: ValorTipo | null;
}

/**
 * Sem precificação gravada, o GET devolve tudo null com versao 0 e updatedAt null.
 * Sem produtos.precificacao.ver_valores_sensiveis, custo/margem/markup/comissão voltam null.
 */
export interface Precificacao extends PrecificacaoDados {
  itemId: string;
  versao: number;
  updatedAt: string | null;
}

export interface PrecificacaoHistorico {
  campoAlterado: keyof PrecificacaoDados;
  valorAnterior: number | string | null;
  valorNovo: number | string | null;
  alteradoPor: string;
  alteradoEm: string;
}

export interface Pagina<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  total: number;
}
