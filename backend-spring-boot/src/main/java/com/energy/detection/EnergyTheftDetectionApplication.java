package com.energy.detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do Sistema de Detecção de Perdas Não Técnicas de Energia.
 * Desenvolvido em Spring Boot com persistência em H2 Database em memória.
 */
@SpringBootApplication
public class EnergyTheftDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyTheftDetectionApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("⚡ Sistema de Detecção de Ligações Clandestinas Iniciado com Sucesso!");
        System.out.println("⚡ API REST: http://localhost:8080/api/energy/summary");
        System.out.println("⚡ H2 Console: http://localhost:8080/h2-console (JDBC: jdbc:h2:mem:energydb)");
        System.out.println("==================================================================");
    }
}
