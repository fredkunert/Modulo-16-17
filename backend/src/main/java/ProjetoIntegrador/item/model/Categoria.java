package ProjetoIntegrador.item.model;
import com.fasterxml.jackson.annotation.*;
import ProjetoIntegrador.comum.RegraException;
public enum Categoria {
    SOFTWARE("SOFTWARE"), LICENCA("LICENÇA"), SERVICO("SERVIÇO"), PROJETO("PROJETO"), CONSULTORIA("CONSULTORIA"),
    BACKUP("BACKUP"), ARMAZENAMENTO("ARMAZENAMENTO"), CLOUD("CLOUD"), SEGURANCA("SEGURANÇA"), ANTIVIRUS("ANTIVÍRUS"),
    EDR("EDR"), XDR("XDR"), MICROSOFT_365("MICROSOFT 365"), GOOGLE_WORKSPACE("GOOGLE WORKSPACE"), SUPORTE("SUPORTE"), OUTROS("OUTROS");
    private final String valor;
    Categoria(String valor) { this.valor=valor; }
    @JsonValue public String valor() { return valor; }
    @JsonCreator public static Categoria from(String valor) {
        for(var categoria:values()) if(categoria.valor.equals(valor)) return categoria;
        throw RegraException.formato("categoria","Categoria inválida. Use uma opção da especificação.");
    }
}
