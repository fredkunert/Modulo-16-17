import type { BadgeVariant } from "@/components/ui";
import type { Categoria } from "@/types/produtos";

const BRL = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const DATA_HORA = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

export const moeda = (v: number | null | undefined) => (v === null || v === undefined ? null : BRL.format(Number(v)));
export const percentual = (v: number | null | undefined) =>
  v === null || v === undefined ? null : `${Number(v).toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;
export const dataHora = (iso: string | null | undefined) => (iso ? DATA_HORA.format(new Date(iso)) : null);

/** "12.5000" com 4 casas → "12,5"; "3.0000" com 0 casas → "3". */
export function quantidade(q: string, casas: number) {
  const n = Number(q);
  if (!Number.isFinite(n)) return q;
  return n.toLocaleString("pt-BR", { minimumFractionDigits: 0, maximumFractionDigits: casas });
}

/** Cores de categoria reutilizando as variantes existentes do Badge. */
export const CATEGORIA_VARIANT: Record<Categoria, BadgeVariant> = {
  SOFTWARE: "blue", "LICENÇA": "indigo", "SERVIÇO": "green", PROJETO: "purple", CONSULTORIA: "purple",
  BACKUP: "blue", ARMAZENAMENTO: "blue", CLOUD: "blue", "SEGURANÇA": "red", "ANTIVÍRUS": "red",
  EDR: "red", XDR: "red", "MICROSOFT 365": "orange", "GOOGLE WORKSPACE": "yellow", SUPORTE: "green", OUTROS: "gray",
};
