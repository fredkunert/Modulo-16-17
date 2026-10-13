package ProjetoIntegrador.comum;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
public final class Patch {
    private Patch() {}
    public static void validar(Map<String,JsonNode> campos, Set<String> permitidos) {
        if(campos==null || campos.isEmpty()) throw RegraException.formato(null,"Informe ao menos um campo.");
        for(String campo:campos.keySet()) if(!permitidos.contains(campo))
            throw new RegraException(400,campo,"CAMPO_NAO_PERMITIDO","Campo não permitido nesta rota.");
    }
    public static String texto(Map<String,JsonNode> campos, String campo, boolean nulo) {
        JsonNode valor=campos.get(campo);
        if(valor==null || valor.isNull()) {
            if(nulo) return null;
            throw RegraException.formato(campo,"O campo não pode ser nulo.");
        }
        if(!valor.isTextual()) throw RegraException.formato(campo,"Informe uma string.");
        return valor.textValue();
    }
    public static String nome(String texto, String campo) {
        if(texto==null || texto.isBlank()) throw RegraException.formato(campo,"Informe um texto não vazio.");
        return texto.strip();
    }
    public static UUID uuid(Map<String,JsonNode> campos, String campo) {
        String valor=texto(campos,campo,true);
        if(valor==null) return null;
        try { UUID id=UUID.fromString(valor); if(!id.toString().equalsIgnoreCase(valor)) throw new IllegalArgumentException(); return id; }
        catch(IllegalArgumentException e) { throw RegraException.formato(campo,"UUID inválido."); }
    }
    public static boolean bool(Map<String,JsonNode> campos,String campo) {
        JsonNode valor=campos.get(campo);
        if(valor==null || !valor.isBoolean()) throw RegraException.formato(campo,"Informe true ou false.");
        return valor.booleanValue();
    }
    public static <E extends Enum<E>> E enumeracao(Map<String,JsonNode> campos,String campo,Class<E> tipo) {
        try { return Enum.valueOf(tipo,texto(campos,campo,false)); }
        catch(IllegalArgumentException e) { throw RegraException.formato(campo,"Enum inválido."); }
    }
}
