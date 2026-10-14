import { useState } from "react";
import { KeyRound, Loader2, PlugZap } from "lucide-react";
import { Button } from "@/components/ui";
import { useSession } from "@/lib/session";
import { TokenModal } from "./HeaderMenus";
import { Logo } from "./Logo";

/** Exibida enquanto não há token válido: tenta o token local de dev e oferece colar um token da equipe. */
export function SessionGate() {
  const { estado, entrarLocal } = useSession();
  const [tentando, setTentando] = useState(false);
  const [tokenModal, setTokenModal] = useState(false);

  const tentar = async () => {
    setTentando(true);
    try { await entrarLocal(); } catch { /* a mensagem fica em estado.mensagem */ } finally { setTentando(false); }
  };

  return (
    <div className="min-h-full flex items-center justify-center bg-brand-950 p-4">
      <div className="w-full max-w-md bg-white rounded-xl shadow-2xl overflow-hidden">
        <div className="px-6 pt-6 pb-5 bg-brand-950 text-white border-b border-brand-900">
          <div className="flex items-center gap-2.5">
            <Logo className="w-12 h-12" />
            <div>
              <p className="font-bold leading-tight">Centinela</p>
              <p className="text-brand-400 text-xs">soluções · Produtos</p>
            </div>
          </div>
        </div>
        <div className="px-6 py-6">
          {estado.status === "carregando" ? (
            <div className="flex items-center gap-3 text-sm text-gray-500">
              <Loader2 className="w-5 h-5 animate-spin text-brand-700" /> Conectando ao backend…
            </div>
          ) : (
            <>
              <div className="flex items-start gap-3">
                <PlugZap className="w-8 h-8 text-orange-500 shrink-0" />
                <div>
                  <p className="font-semibold text-brand-950">Sem sessão ativa</p>
                  <p className="text-sm text-gray-500 mt-1">{estado.status === "erro" ? estado.mensagem : ""}</p>
                </div>
              </div>
              <div className="mt-4 rounded-lg bg-gray-50 border border-gray-100 p-3 text-xs text-gray-600">
                Para testar sem o login do outro grupo, inicie o backend com o perfil local:
                <code className="block mt-1.5 font-mono text-[11px] text-brand-900 break-all">
                  mvn spring-boot:run '-Dspring-boot.run.profiles=local'
                </code>
              </div>
            </>
          )}
        </div>
        <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-2 px-6 py-4 bg-gray-50 border-t border-gray-100">
          <Button variant="outline" onClick={() => setTokenModal(true)}><KeyRound className="w-4 h-4" /> Usar token da equipe</Button>
          <Button onClick={tentar} loading={tentando || estado.status === "carregando"}>Conectar (token local)</Button>
        </div>
      </div>
      <TokenModal open={tokenModal} onClose={() => setTokenModal(false)} />
    </div>
  );
}
