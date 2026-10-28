import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { AlertCircle, Loader2, Package, Search } from "lucide-react";
import { api } from "@/lib/api";
import { useDebounced } from "@/lib/hooks";
import type { BuscaResultado } from "@/types/produtos";

/** Busca global (Ctrl/⌘ + K). Usa GET /busca: somente itens ativos, até 5 resultados. */
export function SearchModal({ open, onClose, onSelect }: { open: boolean; onClose: () => void; onSelect: (r: BuscaResultado) => void }) {
  const [q, setQ] = useState("");
  const termo = useDebounced(q.trim(), 200);
  const [res, setRes] = useState<BuscaResultado[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [ativo, setAtivo] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (!open) return;
    setQ(""); setRes([]); setErro(null); setAtivo(0);
    const t = setTimeout(() => inputRef.current?.focus(), 0);
    return () => clearTimeout(t);
  }, [open]);

  useEffect(() => {
    if (!open || termo.length < 2) { setRes([]); setErro(null); return; }
    let vivo = true;
    setCarregando(true);
    api.busca(termo)
      .then((r) => { if (vivo) { setRes(r); setErro(null); setAtivo(0); } })
      .catch((e: Error) => { if (vivo) { setRes([]); setErro(e.message); } })
      .finally(() => vivo && setCarregando(false));
    return () => { vivo = false; };
  }, [termo, open]);

  if (!open) return null;

  const onKey = (e: KeyboardEvent) => {
    if (e.key === "Escape") onClose();
    else if (e.key === "ArrowDown") { e.preventDefault(); setAtivo((a) => Math.min(a + 1, res.length - 1)); }
    else if (e.key === "ArrowUp") { e.preventDefault(); setAtivo((a) => Math.max(a - 1, 0)); }
    else if (e.key === "Enter" && res[ativo]) onSelect(res[ativo]);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center px-3 pt-[12vh] animate-fade-in" onKeyDown={onKey}>
      <div className="absolute inset-0 bg-brand-950/50 backdrop-blur-[2px]" onClick={onClose} />
      <div role="dialog" aria-modal="true" aria-label="Busca global" className="relative w-full max-w-xl bg-white rounded-xl shadow-2xl overflow-hidden animate-pop-in">
        <div className="flex items-center gap-3 px-4 border-b border-gray-100">
          {carregando ? <Loader2 className="w-5 h-5 text-brand-700 animate-spin shrink-0" /> : <Search className="w-5 h-5 text-gray-400 shrink-0" />}
          <input
            ref={inputRef}
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Buscar itens por nome ou SKU…"
            className="flex-1 h-14 text-base text-gray-800 placeholder:text-gray-400 focus:outline-none bg-transparent"
          />
          <kbd className="text-[10px] font-medium text-gray-400 bg-gray-100 rounded-lg px-1.5 py-0.5">Esc</kbd>
        </div>

        <div className="max-h-[50vh] overflow-y-auto py-2">
          {erro ? (
            <p className="flex items-center gap-2 px-4 py-6 text-sm text-red-600"><AlertCircle className="w-4 h-4 shrink-0" />{erro}</p>
          ) : termo.length < 2 ? (
            <p className="px-4 py-6 text-sm text-gray-400 text-center">Digite ao menos 2 caracteres. A busca retorna até 5 itens ativos.</p>
          ) : !carregando && res.length === 0 ? (
            <p className="px-4 py-6 text-sm text-gray-400 text-center">Nenhum item ativo encontrado para “{termo}”.</p>
          ) : res.length > 0 && (
            <>
              <p className="px-4 pt-1 pb-1.5 text-[11px] font-semibold uppercase tracking-wide text-gray-400">Itens</p>
              {res.map((r, i) => (
                <button
                  key={r.id}
                  onMouseEnter={() => setAtivo(i)}
                  onClick={() => onSelect(r)}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 text-left border-l-2 ${i === ativo ? "bg-brand-50 border-brand-700" : "border-transparent"}`}
                >
                  <Package className="w-4 h-4 text-gray-400 shrink-0" />
                  <span className="min-w-0">
                    <span className="block text-sm font-medium text-brand-950 truncate">{r.titulo}</span>
                    <span className="block text-xs text-gray-400 truncate">{r.subtitulo}</span>
                  </span>
                </button>
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
