import { Factory } from "lucide-react";
import { ParceirosPage } from "@/features/parceiros/ParceirosPage";

export function FabricantesPage({ onAbrirItem }: { onAbrirItem: (id: string) => void }) {
  return (
    <ParceirosPage
      cfg={{ tipo: "fabricantes", titulo: "Fabricantes", singular: "fabricante", icon: <Factory className="w-7 h-7" /> }}
      onAbrirItem={onAbrirItem}
    />
  );
}
