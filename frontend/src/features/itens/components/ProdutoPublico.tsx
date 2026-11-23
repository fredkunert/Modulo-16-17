import { useEffect, useState } from "react";
import { Avatar, DetailField, DetailSection, statusBadge } from "@/components/ui";
import { PERIODICIDADE_LABEL, UNIDADE_LABEL, type Categoria, type Unidade, type Periodicidade } from "@/types/produtos";
import { dataHora, moeda } from "@/lib/format";

interface Produto {
  sku: string; nome: string; categoria: Categoria; descricao: string | null; imagemUrl: string | null;
  unidade: Unidade; periodicidade: Periodicidade; quantidadeMinima: string; ativo: boolean;
  fabricante: string | null; distribuidor: string | null; moeda: string; precoVenda: number | null;
  criadoEm: string; atualizadoEm: string;
}
export function ProdutoPublico({ token }: { token: string }) {
  const [produto, setProduto] = useState<Produto | null>(null);
  const [erro, setErro] = useState("");
  useEffect(() => {
    const abort = new AbortController();
    setProduto(null); setErro("");
    async function carregar() {
      try {
        if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(token)) throw new Error("Link de produto inválido.");
        // Consulta pública pelo gateway, sem token de login.
        const res = await fetch(
          `/public/produtos/itens/${encodeURIComponent(token)}`,{signal: abort.signal,credentials: "omit",cache: "no-store",}
        );
        if (res.status === 404) throw new Error("Este link foi desativado ou o produto não está disponível.");
        if (!res.ok) throw new Error("Não foi possível consultar o produto. Tente novamente mais tarde.");
        const json = await res.json();
        if (!json.success || !json.data) throw new Error("Não foi possível consultar o produto.");
        setProduto(json.data);
      } catch (e) { if (!abort.signal.aborted) setErro(e instanceof TypeError ? "Não foi possível conectar ao sistema. Confira a rede e tente novamente." : (e as Error).message); }
    }
    carregar();
    return () => abort.abort();
  }, [token]);
  return <main className="min-h-full bg-gray-50 px-4 py-8">
    <article className="max-w-2xl mx-auto bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
      <header className="bg-brand-950 text-white px-6 py-5"><p className="text-brand-300 text-xs">Centinela · Catálogo de produtos</p><h1 className="text-xl font-bold mt-1">Informações do produto</h1></header>
      <div className="p-6">
        {erro ? <p role="alert" className="text-red-700">{erro}</p> : !produto ? <p role="status">Carregando produto…</p> : <>
          <div className="flex items-center gap-4 mb-6"><Avatar text={produto.nome} src={produto.imagemUrl} className="w-16 h-16 rounded-xl" /><div className="min-w-0"><h2 className="font-bold text-xl break-words">{produto.nome}</h2><p className="font-mono text-sm text-gray-500 break-all">{produto.sku}</p>{statusBadge(produto.ativo ? "ativo" : "inativo")}</div></div>
          {!produto.ativo && <p className="mb-5 text-amber-800 bg-amber-50 p-3 rounded-lg">Este produto está inativo.</p>}
          <DetailSection number={1} title="Cadastro"><DetailField label="Categoria" value={produto.categoria} /><DetailField label="Descrição" value={produto.descricao} colSpan={2} /></DetailSection>
          <DetailSection number={2} title="Comercialização"><DetailField label="Unidade" value={UNIDADE_LABEL[produto.unidade]} /><DetailField label="Periodicidade" value={PERIODICIDADE_LABEL[produto.periodicidade]} /><DetailField label="Quantidade mínima" value={Number(produto.quantidadeMinima).toLocaleString("pt-BR", { maximumFractionDigits: 4 })} /><DetailField label={`Preço de venda (${produto.moeda})`} value={produto.precoVenda === null ? "Preço não informado" : moeda(produto.precoVenda)} /></DetailSection>
          <DetailSection number={3} title="Parceiros"><DetailField label="Fabricante" value={produto.fabricante} /><DetailField label="Distribuidor" value={produto.distribuidor} /></DetailSection>
          <DetailSection number={4} title="Datas"><DetailField label="Cadastrado em" value={dataHora(produto.criadoEm)} /><DetailField label="Atualizado em" value={dataHora(produto.atualizadoEm)} /></DetailSection>
        </>}
      </div>
    </article>
  </main>;
}
