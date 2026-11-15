import { useEffect, useRef, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { Button, Input, Modal, useToast } from "@/components/ui";
import { api } from "@/lib/api";
import type { Item } from "@/types/produtos";

function enderecoInicial() {
  try { return localStorage.getItem("catalogo.enderecoPublico") || import.meta.env.VITE_PUBLIC_URL || window.location.origin; }
  catch { return import.meta.env.VITE_PUBLIC_URL || window.location.origin; }
}
export function ProdutoQrCode({ item, onClose }: { item: Item; onClose: () => void }) {
  const [endereco, setEndereco] = useState(enderecoInicial);
  const [token, setToken] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(true);
  const [tentativa, setTentativa] = useState(0);
  const [revogado, setRevogado] = useState(false);
  const [erro, setErro] = useState("");
  const ref = useRef<HTMLDivElement>(null);
  const toast = useToast();
  let link = "", aviso = "";
  try {
    const url = new URL(endereco.trim());
    if (!["https:", "http:"].includes(url.protocol) || url.username || url.password) throw new Error();
    const host = url.hostname.toLowerCase();
    if (host === "localhost" || host.endsWith(".localhost") || host.startsWith("127.") || host === "[::1]" || host === "0.0.0.0") aviso = "Para abrir no celular, informe o endereço de rede do computador ou o domínio publicado. localhost só funciona neste computador.";
    url.search = ""; url.hash = `produto=${token ?? ""}`;
    if (token && !aviso) link = url.toString();
  } catch { aviso = "Informe o endereço completo do sistema, começando com http:// ou https://."; }
  useEffect(() => {
    let cancelado = false;
    setOcupado(true); setErro(""); setToken(null); setRevogado(false);
    api.compartilhamento.criar(item.id)
      .then(data => { if (!cancelado) setToken(data.token); })
      .catch(e => { if (!cancelado) setErro(e instanceof Error ? e.message : "Não foi possível preparar o QR Code."); })
      .finally(() => { if (!cancelado) setOcupado(false); });
    return () => { cancelado = true; };
  }, [item.id, tentativa]);
  useEffect(() => {
    if (aviso) return;
    try { localStorage.setItem("catalogo.enderecoPublico", endereco.trim()); } catch { /* armazenamento opcional */ }
  }, [endereco, aviso]);
  async function revogar() {
    setOcupado(true); setErro("");
    try { await api.compartilhamento.revogar(item.id); setToken(null); setRevogado(true); toast.success("Links e QR Codes anteriores deste produto foram desativados."); }
    catch (e) { setErro((e as Error).message); }
    finally { setOcupado(false); }
  }
  function baixar() {
    const svg = ref.current?.querySelector("svg");
    if (!svg) return;
    const url = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(svg)], { type: "image/svg+xml" }));
    const a = document.createElement("a"); a.href = url; a.download = `produto-${item.id}-qr.svg`; a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  return <Modal open onClose={onClose} title="QR Code do produto" subtitle={item.nome}>
    <p className="text-sm text-gray-600 mb-4">Quem tiver o link poderá consultar os dados do produto e o preço de venda, sem login. Custos e margens não são exibidos.</p>
    <label className="block text-sm font-medium mb-1" htmlFor="endereco-publico">Endereço do sistema no celular</label>
    <Input id="endereco-publico" type="url" value={endereco} onChange={e => setEndereco(e.target.value)} placeholder="http://192.168.1.10:5173" />
    {aviso && <p role="status" className="text-sm text-amber-800 mt-2">{aviso}</p>}
    <p className="text-xs text-gray-500 mt-2 mb-4">Em rede local, o celular precisa estar no mesmo Wi-Fi e o sistema deve continuar ligado.</p>
    {erro && <p role="alert" className="text-sm text-red-700 mb-3">{erro}</p>}
    {ocupado && <p role="status" className="text-sm text-gray-600 mb-3">Preparando QR Code…</p>}
    {revogado && <p role="status" className="text-sm text-gray-600 mb-3">Compartilhamento desativado. Reabrir esta janela criará um novo link.</p>}
    <div className="flex gap-2 flex-wrap">
      {erro && !token && <Button onClick={() => setTentativa(t => t + 1)} disabled={ocupado}>Tentar novamente</Button>}
      {revogado && <Button onClick={() => setTentativa(t => t + 1)} disabled={ocupado}>Reativar compartilhamento</Button>}
      <Button variant="outline" onClick={revogar} disabled={ocupado || !token}>Desativar link</Button>
    </div>
    {link && <div className="mt-5 text-center">
      <div ref={ref} className="flex justify-center"><QRCodeSVG value={link} size={240} level="M" marginSize={4} title={`QR Code de ${item.nome}`} /></div>
      <p className="font-mono text-xs text-gray-500 mt-2">{item.sku}</p>
      <div className="mt-3 flex flex-wrap justify-center gap-2"><Button variant="outline" onClick={baixar}>Baixar QR Code</Button><a href={link} target="_blank" rel="noopener noreferrer" className="text-sm text-brand-800 underline p-2">Abrir página pública</a></div>
      <label htmlFor="link-produto" className="block text-xs text-gray-500 mt-3 mb-1">Link para compartilhar</label><Input id="link-produto" readOnly value={link} onFocus={e => e.target.select()} />
    </div>}
  </Modal>;
}
