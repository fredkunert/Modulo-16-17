import { useState } from "react";
import { Ban, BadgeDollarSign, Copy, Pencil, RotateCcw, X } from "lucide-react";
import { Badge, Button, DetailField, DetailSection, statusBadge, useToast } from "@/components/ui";
import { CATEGORIA_VARIANT, dataHora, moeda, percentual, quantidade } from "@/lib/format";
import { PERIODICIDADE_LABEL, UNIDADE_LABEL, type Item, type Parceiro } from "@/types/produtos";
import { useSession } from "@/lib/session";
import { useParceiro } from "@/lib/useParceiros";
import { PrecificacaoTab } from "./PrecificacaoTab";

const TABS = ["Visão geral", "Precificação", "Registro"];

export function ItemDetailPanel({
  item, fabricante, distribuidor, podeEditar, onEdit, onToggleAtivo, onPrecificacaoSalva, onClose,
}: {
  item: Item;
  fabricante: Parceiro | null;
  distribuidor: Parceiro | null;
  podeEditar: boolean;
  onEdit: () => void;
  onToggleAtivo: () => void;
  /** Chamado após salvar a precificação: o preço gravado também aparece no item do catálogo. */
  onPrecificacaoSalva: () => void;
  onClose: () => void;
}) {
  const [tab, setTab] = useState(0);
  const toast = useToast();
  const podeVerPrecificacao = useSession().can("produtos.precificacao.ver");
  const fab = useParceiro("fabricantes", item.fabricanteId, fabricante);
  const dist = useParceiro("distribuidores", item.distribuidorId, distribuidor);
  const semPreco = item.precoVenda === null && item.precoMinimo === null && item.descontoMaximo === null;

  const copiarId = async () => {
    try { await navigator.clipboard.writeText(item.id); toast.success("ID copiado."); }
    catch { toast.error("Não foi possível copiar o ID."); }
  };

  const parceiro = (p: Parceiro | null, id: string | null) =>
    !id ? null : p ? <span className="inline-flex items-center gap-1.5">{p.nome}{!p.ativo && statusBadge("inativo")}</span>
      : <span className="font-mono text-xs text-gray-500">{id}</span>;

  return (
    <div className="flex flex-col overflow-hidden bg-white w-full min-h-0">
      <div className="px-5 pt-5 pb-4 bg-brand-950 text-white shrink-0">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-brand-400 text-xs font-mono font-semibold">{item.sku}</p>
            <h2 className="font-bold text-lg leading-snug mt-0.5">{item.nome}</h2>
            <div className="flex flex-wrap items-center gap-1.5 mt-2">
              <Badge variant={CATEGORIA_VARIANT[item.categoria]}>{item.categoria}</Badge>
              {statusBadge(item.ativo ? "ativo" : "inativo")}
              <span className="text-brand-300 text-xs">
                {UNIDADE_LABEL[item.unidade]} · {PERIODICIDADE_LABEL[item.periodicidade]}
              </span>
            </div>
          </div>
          <div className="flex gap-1.5 shrink-0">
            {podeEditar && (
              <>
                <Button variant="secondary" size="sm" onClick={onEdit}>
                  <Pencil className="w-3 h-3" /> Editar
                </Button>
                <Button variant="secondary" size="sm" onClick={onToggleAtivo} title={item.ativo ? "Inativar" : "Reativar"}>
                  {item.ativo ? <Ban className="w-3 h-3" /> : <RotateCcw className="w-3 h-3" />}
                  <span className="hidden sm:inline">{item.ativo ? "Inativar" : "Reativar"}</span>
                </Button>
              </>
            )}
            <button onClick={onClose} aria-label="Fechar" className="p-1.5 rounded-lg hover:bg-brand-800 text-brand-300 hover:text-white">
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      <div className="flex border-b border-gray-100 bg-white shrink-0 overflow-x-auto">
        {TABS.map((t, i) => (
          <button
            key={t}
            onClick={() => setTab(i)}
            className={`px-4 py-2.5 text-xs font-semibold whitespace-nowrap border-b-2 transition-colors
              ${tab === i ? "border-brand-700 text-brand-800" : "border-transparent text-gray-500 hover:text-gray-700"}`}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-5">
        {tab === 0 && (
          <>
            <DetailSection number={1} title="Identificação">
              <DetailField label="SKU" value={item.sku} mono />
              <DetailField label="Categoria" value={item.categoria} />
              <DetailField label="Nome" value={item.nome} colSpan={2} />
              <DetailField label="Descrição" value={item.descricao} colSpan={2} />
            </DetailSection>
            <DetailSection number={2} title="Comercialização">
              <DetailField label="Unidade" value={UNIDADE_LABEL[item.unidade]} />
              <DetailField label="Periodicidade" value={PERIODICIDADE_LABEL[item.periodicidade]} />
              <DetailField
                label="Quantidade mínima"
                value={`${quantidade(item.quantidadeMinima, item.casasDecimaisQuantidade)} ${UNIDADE_LABEL[item.unidade].toLowerCase()}`}
              />
              <DetailField label="Casas decimais aceitas" value={String(item.casasDecimaisQuantidade)} />
            </DetailSection>
            <DetailSection number={3} title="Parceiros">
              <DetailField label="Fabricante" value={parceiro(fab, item.fabricanteId)} />
              <DetailField label="Distribuidor" value={parceiro(dist, item.distribuidorId)} />
            </DetailSection>
          </>
        )}

        {tab === 1 && (
          podeVerPrecificacao ? (
            <PrecificacaoTab itemId={item.id} moedaItem={item.moeda} onSalvo={onPrecificacaoSalva} />
          ) : semPreco ? (
            <div className="flex flex-col items-center text-center py-10">
              <div className="w-14 h-14 rounded-full bg-gray-100 text-gray-400 flex items-center justify-center mb-3">
                <BadgeDollarSign className="w-7 h-7" />
              </div>
              <p className="font-semibold text-brand-950">Sem precificação publicada</p>
              <p className="text-sm text-gray-500 mt-1 max-w-sm">
                Os valores são definidos pelo módulo de Precificação. Enquanto não houver publicação, eles permanecem vazios.
              </p>
            </div>
          ) : (
            <DetailSection number={1} title={`Valores publicados (${item.moeda})`}>
              <DetailField label="Preço de venda" value={moeda(item.precoVenda)} />
              <DetailField label="Preço mínimo" value={moeda(item.precoMinimo)} />
              <DetailField label="Desconto máximo" value={percentual(item.descontoMaximo)} />
            </DetailSection>
          )
        )}

        {tab === 2 && (
          <DetailSection number={1} title="Registro">
            <DetailField
              label="ID"
              colSpan={2}
              value={
                <span className="inline-flex items-center gap-2">
                  <span className="font-mono text-xs">{item.id}</span>
                  <button onClick={copiarId} className="p-1 rounded text-gray-400 hover:text-brand-800 hover:bg-brand-50" aria-label="Copiar ID">
                    <Copy className="w-3 h-3" />
                  </button>
                </span>
              }
            />
            <DetailField label="Criado em" value={dataHora(item.criadoEm)} />
            <DetailField label="Atualizado em" value={dataHora(item.atualizadoEm)} />
          </DetailSection>
        )}
      </div>
    </div>
  );
}
