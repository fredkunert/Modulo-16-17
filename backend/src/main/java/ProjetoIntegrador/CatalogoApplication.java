package ProjetoIntegrador;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@SpringBootApplication(scanBasePackages = {"ProjetoIntegrador", "br.edu.pi2026.produtos"})
@EntityScan(basePackages = {"ProjetoIntegrador", "br.edu.pi2026.produtos"})
@EnableJpaRepositories(basePackages = {"ProjetoIntegrador", "br.edu.pi2026.produtos"})
public class CatalogoApplication {
    public static void main(String[] args) { SpringApplication.run(CatalogoApplication.class, args); }
}
