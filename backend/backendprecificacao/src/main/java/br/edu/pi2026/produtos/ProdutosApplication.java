package br.edu.pi2026.produtos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Serviço de precificação (porta 8084). Os pacotes abaixo deste são varridos automaticamente. */
@SpringBootApplication
public class ProdutosApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProdutosApplication.class, args);
    }
}
