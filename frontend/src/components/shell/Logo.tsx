/** Selo da Centinela (public/logo.centinela.png). A imagem tem margem branca; o zoom recorta só o selo. */
export function Logo({ className = "w-9 h-9" }: { className?: string }) {
  return (
    <span className={`rounded-full bg-white overflow-hidden flex items-center justify-center shrink-0 ${className}`}>
      <img src="/logo.centinela.png" alt="Centinela Soluções" className="w-full h-full object-cover scale-[1.3]" />
    </span>
  );
}
