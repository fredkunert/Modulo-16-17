package ProjetoIntegrador.seguranca;
import ProjetoIntegrador.comum.RegraException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
@Component
public class ContextoUsuario {
    private final HttpServletRequest request;
    public ContextoUsuario(HttpServletRequest request) { this.request=request; }
    private Jwt token() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !(auth.getPrincipal() instanceof Jwt jwt))
            throw new RegraException(401,null,"TOKEN_INVALIDO","Token inválido.");
        return jwt;
    }
    private boolean servico(Jwt jwt) { return jwt.getSubject()!=null && jwt.getSubject().startsWith("svc:"); }
    public String tenant() {
        Jwt jwt=token();
        String tenant=servico(jwt)?request.getHeader("X-Tenant-Id"):jwt.getClaimAsString("tenant_id");
        if(tenant==null || tenant.isBlank() || tenant.length()>255)
            throw new RegraException(servico(jwt)?400:401,"tenant_id","TENANT_OBRIGATORIO","Contexto de tenant ausente ou inválido.");
        return tenant;
    }
    public String autor() {
        Jwt jwt=token();
        String autor=servico(jwt)?jwt.getClaimAsString("clientId"):jwt.getSubject();
        if(autor==null || autor.isBlank() || autor.length()>255)
            throw new RegraException(401,null,"AUTOR_INVALIDO","Identidade de auditoria ausente ou inválida.");
        return autor;
    }
}
