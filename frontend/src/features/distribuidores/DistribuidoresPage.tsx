import { Truck } from "lucide-react";
import { ParceirosPage } from "@/features/parceiros/ParceirosPage";

export function DistribuidoresPage({ onAbrirItem }: { onAbrirItem: (id: string) => void }) {
  return (
    <ParceirosPage
      cfg={{ tipo: "distribuidores", titulo: "Distribuidores", singular: "distribuidor", icon: <Truck className="w-7 h-7" /> }}
      onAbrirItem={onAbrirItem}
    />
  );
}
