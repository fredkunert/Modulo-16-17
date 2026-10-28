import { Factory, Truck } from "lucide-react";
import { Avatar, Badge, statusBadge } from "@/components/ui";
import { CATEGORIA_VARIANT, moeda, quantidade } from "@/lib/format";
import { PERIODICIDADE_LABEL, UNIDADE_LABEL, type Item, type Parceiro } from "@/types/produtos";

interface Props {
  itens: Item[];
  porId: (id: string | null) => Parceiro | null;
  onSelect: (i: Item) => void;
}

export function ItemTable({ itens, porId, onSelect }: Props) {
  return (
    <div className="bg-white rounded-xl border border-gray-100 shadow-sm overflow-x-auto">
      <table className="w-full text-sm min-w-[860px]">
        <thead>
          <tr className="border-b border-gray-100 text-left text-[11px] uppercase tracking-wide text-gray-500 font-semibold">
            <th className="px-4 py-3 font-semibold">Item</th>
            <th className="px-4 py-3 font-semibold">Categoria</th>
            <th className="px-4 py-3 font-semibold">Unidade · Período</th>
            <th className="px-4 py-3 font-semibold text-right">Qtd. mín.</th>
            <th className="px-4 py-3 font-semibold">Fabricante</th>
            <th className="px-4 py-3 font-semibold text-right">Preço venda</th>
            <th className="px-4 py-3 font-semibold">Situação</th>
          </tr>
        </thead>
        <tbody>
          {itens.map((i) => (
            <tr
              key={i.id}
              onClick={() => onSelect(i)}
              className={`border-b border-gray-100 last:border-0 cursor-pointer hover:bg-brand-50 transition-colors ${i.ativo ? "" : "opacity-60"}`}
            >
              <td className="px-4 py-3">
                <div className="flex items-center gap-3 min-w-0">
                  <Avatar text={i.nome} />
                  <div className="min-w-0">
                    <p className="font-medium text-brand-950 truncate max-w-[280px]">{i.nome}</p>
                    <p className="text-xs text-gray-400 font-mono">{i.sku}</p>
                  </div>
                </div>
              </td>
              <td className="px-4 py-3"><Badge variant={CATEGORIA_VARIANT[i.categoria]}>{i.categoria}</Badge></td>
              <td className="px-4 py-3 text-gray-700">{UNIDADE_LABEL[i.unidade]} · {PERIODICIDADE_LABEL[i.periodicidade]}</td>
              <td className="px-4 py-3 text-gray-700 text-right tabular-nums">{quantidade(i.quantidadeMinima, i.casasDecimaisQuantidade)}</td>
              <td className="px-4 py-3 text-gray-700">{porId(i.fabricanteId)?.nome ?? <span className="text-gray-300">—</span>}</td>
              <td className="px-4 py-3 text-right tabular-nums">
                {i.precoVenda !== null ? <span className="text-gray-800 font-medium">{moeda(i.precoVenda)}</span> : statusBadge("sem preço")}
              </td>
              <td className="px-4 py-3">{statusBadge(i.ativo ? "ativo" : "inativo")}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function ItemCards({ itens, porId, onSelect }: Props) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
      {itens.map((i) => {
        const fab = porId(i.fabricanteId);
        const dist = porId(i.distribuidorId);
        return (
          <button
            key={i.id}
            onClick={() => onSelect(i)}
            className={`text-left bg-white rounded-xl border border-gray-100 shadow-sm hover:shadow-md hover:border-brand-200 transition-all p-4 flex flex-col ${i.ativo ? "" : "opacity-60"}`}
          >
            <div className="flex items-start justify-between gap-2">
              <Avatar text={i.nome} className="w-10 h-10 rounded-lg" />
              {statusBadge(i.ativo ? "ativo" : "inativo")}
            </div>
            <p className="font-semibold text-brand-950 mt-3 leading-snug line-clamp-2">{i.nome}</p>
            <p className="text-xs text-gray-400 font-mono mt-0.5">{i.sku}</p>
            <p className="text-xs text-gray-500 mt-1">
              {quantidade(i.quantidadeMinima, i.casasDecimaisQuantidade)} {UNIDADE_LABEL[i.unidade].toLowerCase()} mín. · {PERIODICIDADE_LABEL[i.periodicidade]}
            </p>
            <div className="flex flex-wrap gap-1.5 mt-2 mb-3">
              <Badge variant={CATEGORIA_VARIANT[i.categoria]}>{i.categoria}</Badge>
              {fab && <Badge variant="green"><Factory className="w-3 h-3" />{fab.nome}</Badge>}
              {dist && <Badge variant="green"><Truck className="w-3 h-3" />{dist.nome}</Badge>}
            </div>
            <div className="flex items-center justify-between mt-auto pt-3 border-t border-gray-100">
              <span className="text-xs text-gray-400">Preço de venda</span>
              {i.precoVenda !== null ? <span className="text-sm font-semibold text-brand-950">{moeda(i.precoVenda)}</span> : statusBadge("sem preço")}
            </div>
          </button>
        );
      })}
    </div>
  );
}
