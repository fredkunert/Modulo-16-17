import { useEffect, useRef, useState, type ReactNode } from "react";
import { Bell, CheckCircle2, Clock, KeyRound, LogOut, RefreshCw, ServerCrash, ShieldAlert, UserCog } from "lucide-react";
import { Badge, Button, FormField, Modal, Textarea, useToast } from "@/components/ui";
import { useSession, type PerfilLocal } from "@/lib/session";

function useClickOutside(open: boolean, close: () => void) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) close(); };
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && close();
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => { document.removeEventListener("mousedown", onDown); document.removeEventListener("keydown", onKey); };
  }, [open, close]);
  return ref;
}

function Dropdown({ open, children, className = "w-80" }: { open: boolean; children: ReactNode; className?: string }) {
  if (!open) return null;
  return (
    <div className={`absolute right-0 top-full mt-2 z-40 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden animate-pop-in ${className}`}>
      {children}
    </div>
  );
}

export interface Aviso { id: string; icon: ReactNode; texto: string; quando: string }

/** Avisos derivados do estado do sistema (conectividade do backend e validade do token). */
export function NotificationsMenu({ backendOk }: { backendOk: boolean | null }) {
  const { sessao } = useSession();
  const [open, setOpen] = useState(false);
  const [lidos, setLidos] = useState<Set<string>>(new Set());
  const ref = useClickOutside(open, () => setOpen(false));
  const [agora, setAgora] = useState(Date.now());
  useEffect(() => { const t = setInterval(() => setAgora(Date.now()), 30_000); return () => clearInterval(t); }, []);

  const avisos: Aviso[] = [];
  if (backendOk === false) avisos.push({ id: "backend", icon: <ServerCrash className="w-4 h-4 text-red-600 shrink-0" />, texto: "Backend indisponível ou sem acesso ao banco de dados.", quando: "agora" });
  if (sessao) {
    const min = Math.round((sessao.expiraEm - agora) / 60_000);
    if (min <= 10) avisos.push({ id: `exp-${sessao.expiraEm}`, icon: <Clock className="w-4 h-4 text-orange-600 shrink-0" />, texto: `Token expira em ${Math.max(min, 0)} min.${sessao.origem === "local" ? " Será renovado automaticamente." : ""}`, quando: "agora" });
    if (!sessao.permissoes.includes("produtos.item.ver")) avisos.push({ id: "perm", icon: <ShieldAlert className="w-4 h-4 text-purple-600 shrink-0" />, texto: "Perfil restrito: apenas busca e resumos estão disponíveis.", quando: "sessão" });
  }
  if (backendOk) avisos.push({ id: "ok", icon: <CheckCircle2 className="w-4 h-4 text-brand-700 shrink-0" />, texto: "Backend conectado ao banco de dados.", quando: "status" });

  const naoLidos = avisos.filter((a) => a.id !== "ok" && !lidos.has(a.id));

  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} className="relative p-2 rounded-lg text-gray-500 hover:bg-gray-100 hover:text-gray-800" aria-label="Notificações">
        <Bell className="w-5 h-5" />
        {naoLidos.length > 0 && (
          <span className="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-red-600 text-white text-[10px] font-bold flex items-center justify-center">
            {naoLidos.length}
          </span>
        )}
      </button>
      <Dropdown open={open}>
        <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
          <div className="flex items-center gap-2">
            <span className="font-bold text-brand-950 text-sm">Notificações</span>
            {naoLidos.length > 0 && <span className="min-w-5 h-5 px-1.5 rounded-full bg-red-600 text-white text-[11px] font-bold flex items-center justify-center">{naoLidos.length}</span>}
          </div>
          {naoLidos.length > 0 && (
            <button onClick={() => setLidos(new Set(avisos.map((a) => a.id)))} className="text-xs font-medium text-blue-600 hover:underline">
              Marcar tudo como lido
            </button>
          )}
        </div>
        <ul className="max-h-80 overflow-y-auto divide-y divide-gray-50">
          {avisos.length === 0 && <li className="px-4 py-6 text-sm text-gray-400 text-center">Nenhuma notificação.</li>}
          {avisos.map((a) => {
            const novo = a.id !== "ok" && !lidos.has(a.id);
            return (
              <li key={a.id} className={`flex items-start gap-3 px-4 py-3 ${novo ? "" : "opacity-60"}`}>
                <span className="mt-0.5">{a.icon}</span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-700">{a.texto}</p>
                  <p className="text-xs text-gray-400 mt-0.5">{a.quando}</p>
                </div>
                {novo && <span className="w-2 h-2 rounded-full bg-blue-500 mt-1.5 shrink-0" />}
              </li>
            );
          })}
        </ul>
      </Dropdown>
    </div>
  );
}

export function UserMenu() {
  const { sessao, entrarLocal, sair } = useSession();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [tokenModal, setTokenModal] = useState(false);
  const ref = useClickOutside(open, () => setOpen(false));
  if (!sessao) return null;

  const iniciais = sessao.usuario.replace(/[^a-zA-Z]/g, " ").trim().split(/\s+/).map((p) => p[0]).slice(0, 2).join("").toUpperCase() || "U";

  const trocarPerfil = async (perfil: PerfilLocal) => {
    try { await entrarLocal(perfil, sessao.tenantId); toast.success(`Perfil "${perfil}" ativo.`); setOpen(false); }
    catch (e) { toast.error((e as Error).message); }
  };

  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen((o) => !o)} className="w-8 h-8 rounded-full bg-brand-700 text-white text-xs font-semibold flex items-center justify-center hover:ring-2 hover:ring-brand-200" aria-label="Conta">
        {iniciais}
      </button>
      <Dropdown open={open} className="w-80">
        <div className="px-4 py-3 border-b border-gray-100">
          <p className="text-sm font-bold text-brand-950 truncate">{sessao.usuario}</p>
          <p className="text-xs text-gray-500 truncate">Tenant: <span className="font-mono">{sessao.tenantId}</span></p>
          <p className="text-xs text-gray-400 mt-0.5">
            {sessao.origem === "local" ? `Token local de desenvolvimento · perfil ${sessao.perfil}` : "Token informado manualmente"}
          </p>
          <div className="flex flex-wrap gap-1 mt-2">
            {sessao.permissoes.map((p) => <Badge key={p} variant="green">{p.replace("produtos.item.", "")}</Badge>)}
          </div>
        </div>
        <div className="p-2 flex flex-col">
          {sessao.origem === "local" && (
            <>
              <MenuItem icon={<RefreshCw className="w-4 h-4" />} onClick={() => trocarPerfil(sessao.perfil ?? "catalogo")}>Renovar token</MenuItem>
              <MenuItem icon={<UserCog className="w-4 h-4" />} onClick={() => trocarPerfil(sessao.perfil === "resumo" ? "catalogo" : "resumo")}>
                Usar perfil {sessao.perfil === "resumo" ? "catálogo (completo)" : "resumo (restrito)"}
              </MenuItem>
            </>
          )}
          <MenuItem icon={<KeyRound className="w-4 h-4" />} onClick={() => { setOpen(false); setTokenModal(true); }}>Usar token da equipe</MenuItem>
          <MenuItem icon={<LogOut className="w-4 h-4" />} onClick={sair} danger>Encerrar sessão</MenuItem>
        </div>
      </Dropdown>
      <TokenModal open={tokenModal} onClose={() => setTokenModal(false)} />
    </div>
  );
}

function MenuItem({ icon, children, onClick, danger }: { icon: ReactNode; children: ReactNode; onClick: () => void; danger?: boolean }) {
  return (
    <button onClick={onClick} className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-left ${danger ? "text-red-600 hover:bg-red-50" : "text-gray-700 hover:bg-gray-50"}`}>
      <span className="shrink-0">{icon}</span>{children}
    </button>
  );
}

export function TokenModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { entrarComToken } = useSession();
  const toast = useToast();
  const [token, setToken] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  useEffect(() => { if (open) { setToken(""); setErro(null); } }, [open]);

  const confirmar = () => {
    try {
      entrarComToken(token.replace(/^Bearer\s+/i, ""));
      toast.success("Token aplicado.");
      onClose();
    } catch {
      setErro("Token inválido. Cole o JWT completo (três partes separadas por ponto).");
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="md"
      title="Usar token da equipe"
      subtitle="Cole um JWT emitido pelo provedor de autenticação do grupo"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>Cancelar</Button>
          <Button onClick={confirmar} disabled={!token.trim()}><KeyRound className="w-4 h-4" /> Aplicar</Button>
        </>
      }
    >
      <FormField label="Access token (JWT)" required error={erro ?? undefined} hint="O token fica salvo somente neste navegador até expirar.">
        <Textarea value={token} onChange={(e) => { setToken(e.target.value); setErro(null); }} invalid={!!erro} className="font-mono text-xs min-h-[140px]" placeholder="eyJhbGciOiJSUzI1NiIs…" />
      </FormField>
    </Modal>
  );
}
