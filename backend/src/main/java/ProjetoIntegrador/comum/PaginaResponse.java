package ProjetoIntegrador.comum;
import java.util.List;
import org.springframework.data.domain.Page;
public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long total) {
    public static <T> PaginaResponse<T> from(Page<T> page) {
        return new PaginaResponse<>(page.getContent(),page.getNumber(),page.getSize(),page.getTotalElements());
    }
}
