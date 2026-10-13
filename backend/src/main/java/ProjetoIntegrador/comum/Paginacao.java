package ProjetoIntegrador.comum;
import org.springframework.data.domain.*;
public final class Paginacao {
    private Paginacao() {}
    public static Pageable criar(int pagina, int tamanho, String ordenar) {
        if(pagina<0 || tamanho<1 || tamanho>100) throw RegraException.formato("pagina/tamanho","Página deve ser >= 0 e tamanho entre 1 e 100.");
        String[] partes=ordenar.split(",",-1);
        if(partes.length!=2 || !partes[0].equals("nome") || !(partes[1].equals("asc") || partes[1].equals("desc")))
            throw RegraException.formato("ordenar","Use nome,asc ou nome,desc.");
        return PageRequest.of(pagina,tamanho,Sort.by(Sort.Direction.fromString(partes[1]),"nome").and(Sort.by("id")));
    }
}
