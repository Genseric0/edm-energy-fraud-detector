package com.energy.detection.config;

import com.energy.detection.model.Meter;
import com.energy.detection.model.Transformer;
import com.energy.detection.repository.TransformerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Data Seeder para inicialização da base de dados H2 (EDM - Electricidade de Moçambique).
 * Popula a rede de distribuição com Postos de Transformação (PT) e Medidores Residenciais
 * localizados na cidade de Maputo (Zimpeto, Xipamanine, Mafalala, Polana Cimento, Alto Maé e Malhangalene).
 *
 * Configuração estrita de localização:
 * - Moeda: Metical Moçambicano (MT / MZN)
 * - Nomes e apelidos tradicionais moçambicanos
 * - Contactos telefónicos nacionais (+258)
 * - Simulação de perdas normais (perda <= 5%) e fraudes críticas (perda > 20%)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final TransformerRepository transformerRepository;

    @Override
    public void run(String... args) {
        if (transformerRepository.count() > 0) {
            log.info("Base de dados de distribuição da EDM já se encontra populada.");
            return;
        }

        log.info("Iniciando população de dados da rede de distribuição EDM (Maputo, Moçambique)...");

        // =========================================================================
        // 1. POSTO DE TRANSFORMAÇÃO: "PT-Zimpeto-01" (Bairro do Zimpeto, Maputo)
        // Setor em conformidade técnica (Perda ~3,31% <= 5,0% tolerância EDM)
        // =========================================================================
        Transformer pt1 = Transformer.builder()
                .code("PT-Zimpeto-01")
                .neighborhood("Zimpeto")
                .capacityKva(100.0)
                .totalSuppliedKwh(14500.0) // Energia Injetada pelo PT
                .build();

        List<Meter> metersPt1 = Arrays.asList(
                Meter.builder().meterNumber("MED-MZ-1011").residentName("Albino Tembe").phoneNumber("+258 84 321 8901").address("Av. de Moçambique, 104").billedKwh(2050.0).transformer(pt1).build(),
                Meter.builder().meterNumber("MED-MZ-1012").residentName("Graça Cossa").phoneNumber("+258 82 455 1234").address("Av. de Moçambique, 118").billedKwh(2100.0).transformer(pt1).build(),
                Meter.builder().meterNumber("MED-MZ-1013").residentName("Ernesto Mondlane").phoneNumber("+258 87 610 9876").address("Rua do Estádio do Zimpeto, 45").billedKwh(1950.0).transformer(pt1).build(),
                Meter.builder().meterNumber("MED-MZ-1014").residentName("Lurdes Pelembe").phoneNumber("+258 84 890 2345").address("Rua do Estádio do Zimpeto, 53").billedKwh(2300.0).transformer(pt1).build(),
                Meter.builder().meterNumber("MED-MZ-1015").residentName("Zacarias Muianga").phoneNumber("+258 82 771 5432").address("Rua da Feira do Zimpeto, 12").billedKwh(2000.0).transformer(pt1).build(),
                Meter.builder().meterNumber("MED-MZ-1016").residentName("Fátima Macamo").phoneNumber("+258 84 123 7890").address("Rua da Feira do Zimpeto, 20").billedKwh(1800.0).transformer(pt1).build(),
                Meter.builder().meterNumber("MED-MZ-1017").residentName("Mateus Nhavoto").phoneNumber("+258 87 404 6543").address("2ª Circular de Maputo, 88").billedKwh(1820.0).transformer(pt1).build()
        );
        // Soma Medidores = 14.020 kWh | Fornecido = 14.500 kWh | Perda = 480 kWh (3,31%) -> NORMAL
        pt1.setMeters(metersPt1);

        // =========================================================================
        // 2. POSTO DE TRANSFORMAÇÃO: "PT-Xipamanine-02" (Bairro do Xipamanine, Maputo)
        // FRAUDE CRÍTICA: Perda de 40,40% > 5,0% (Suspeita de Grande Ligação Clandestina / Bypass)
        // =========================================================================
        Transformer pt2 = Transformer.builder()
                .code("PT-Xipamanine-02")
                .neighborhood("Xipamanine")
                .capacityKva(160.0)
                .totalSuppliedKwh(19800.0) // Energia Injetada alta
                .build();

        List<Meter> metersPt2 = Arrays.asList(
                Meter.builder().meterNumber("MED-MZ-2021").residentName("Armando Langa").phoneNumber("+258 84 555 4321").address("Rua dos Irmãos Roby, 14").billedKwh(1750.0).transformer(pt2).build(),
                Meter.builder().meterNumber("MED-MZ-2022").residentName("Amina Sitoe").phoneNumber("+258 82 998 1122").address("Rua dos Irmãos Roby, 22").billedKwh(1600.0).transformer(pt2).build(),
                Meter.builder().meterNumber("MED-MZ-2023").residentName("Abdul Matsinhe").phoneNumber("+258 87 333 9988").address("Av. de Angola, 305").billedKwh(450.0).transformer(pt2).build(), // Bypass suspeito
                Meter.builder().meterNumber("MED-MZ-2024").residentName("Teresa Mabunda").phoneNumber("+258 84 667 8899").address("Av. de Angola, 312").billedKwh(1900.0).transformer(pt2).build(),
                Meter.builder().meterNumber("MED-MZ-2025").residentName("Samora Zandamela").phoneNumber("+258 82 222 6677").address("Travessa do Mercado, 08").billedKwh(1800.0).transformer(pt2).build(),
                Meter.builder().meterNumber("MED-MZ-2026").residentName("Joana Chissano").phoneNumber("+258 84 777 3344").address("Travessa do Mercado, 15").billedKwh(520.0).transformer(pt2).build(), // Bypass suspeito
                Meter.builder().meterNumber("MED-MZ-2027").residentName("Félix Nhantumbo").phoneNumber("+258 87 888 2211").address("Rua da OMM, 42").billedKwh(1950.0).transformer(pt2).build(),
                Meter.builder().meterNumber("MED-MZ-2028").residentName("Esperança Guambe").phoneNumber("+258 84 119 5566").address("Rua da OMM, 50").billedKwh(1830.0).transformer(pt2).build()
        );
        // Soma Medidores = 11.800 kWh | Fornecido = 19.800 kWh | Perda = 8.000 kWh (40,40% > 20%) -> ALERTA CRÍTICO
        pt2.setMeters(metersPt2);

        // =========================================================================
        // 3. POSTO DE TRANSFORMAÇÃO: "PT-Mafalala-01" (Bairro da Mafalala, Maputo)
        // FRAUDE CRÍTICA: Perda de 43,03% > 5,0% (Furto massivo e adulteração de medidores)
        // =========================================================================
        Transformer pt3 = Transformer.builder()
                .code("PT-Mafalala-01")
                .neighborhood("Mafalala")
                .capacityKva(100.0)
                .totalSuppliedKwh(16500.0)
                .build();

        List<Meter> metersPt3 = Arrays.asList(
                Meter.builder().meterNumber("MED-MZ-3031").residentName("Helder Magaia").phoneNumber("+258 84 444 3322").address("Rua de Goa, 23").billedKwh(1650.0).transformer(pt3).build(),
                Meter.builder().meterNumber("MED-MZ-3032").residentName("Palmira Muchanga").phoneNumber("+258 82 765 4321").address("Rua de Goa, 31").billedKwh(1700.0).transformer(pt3).build(),
                Meter.builder().meterNumber("MED-MZ-3033").residentName("Ismael Tembe").phoneNumber("+258 87 901 2345").address("Rua da Mafalala, 102").billedKwh(380.0).transformer(pt3).build(), // Bypass suspeito
                Meter.builder().meterNumber("MED-MZ-3034").residentName("Cecília Cossa").phoneNumber("+258 84 312 7788").address("Rua da Mafalala, 110").billedKwh(1800.0).transformer(pt3).build(),
                Meter.builder().meterNumber("MED-MZ-3035").residentName("Rui Mondlane").phoneNumber("+258 82 654 9870").address("Beco das Flores, 04").billedKwh(1550.0).transformer(pt3).build(),
                Meter.builder().meterNumber("MED-MZ-3036").residentName("Hortência Pelembe").phoneNumber("+258 84 789 0123").address("Beco das Flores, 11").billedKwh(420.0).transformer(pt3).build(), // Bypass suspeito
                Meter.builder().meterNumber("MED-MZ-3037").residentName("Tomás Macamo").phoneNumber("+258 87 234 5678").address("Av. Acordos de Lusaka, 89").billedKwh(1900.0).transformer(pt3).build()
        );
        // Soma Medidores = 9.400 kWh | Fornecido = 16.500 kWh | Perda = 7.100 kWh (43,03% > 20%) -> ALERTA CRÍTICO
        pt3.setMeters(metersPt3);

        // =========================================================================
        // 4. POSTO DE TRANSFORMAÇÃO: "PT-Polana-01" (Bairro Polana Cimento, Maputo)
        // Setor em conformidade técnica (Perda ~3,30% <= 5,0%)
        // =========================================================================
        Transformer pt4 = Transformer.builder()
                .code("PT-Polana-01")
                .neighborhood("Polana Cimento")
                .capacityKva(160.0)
                .totalSuppliedKwh(18200.0)
                .build();

        List<Meter> metersPt4 = Arrays.asList(
                Meter.builder().meterNumber("MED-MZ-4041").residentName("Belmiro Nhavoto").phoneNumber("+258 84 987 6543").address("Av. Julius Nyerere, 1205").billedKwh(3100.0).transformer(pt4).build(),
                Meter.builder().meterNumber("MED-MZ-4042").residentName("Clotilde Muianga").phoneNumber("+258 82 123 4567").address("Av. Julius Nyerere, 1215").billedKwh(2950.0).transformer(pt4).build(),
                Meter.builder().meterNumber("MED-MZ-4043").residentName("Domingos Langa").phoneNumber("+258 87 456 7890").address("Av. Armando Tivane, 340").billedKwh(2850.0).transformer(pt4).build(),
                Meter.builder().meterNumber("MED-MZ-4044").residentName("Eunice Sitoe").phoneNumber("+258 84 345 6789").address("Av. Armando Tivane, 352").billedKwh(3050.0).transformer(pt4).build(),
                Meter.builder().meterNumber("MED-MZ-4045").residentName("Gilberto Matsinhe").phoneNumber("+258 82 567 8901").address("Rua Mateus Sansão Muthemba, 88").billedKwh(2750.0).transformer(pt4).build(),
                Meter.builder().meterNumber("MED-MZ-4046").residentName("Helena Cossa").phoneNumber("+258 84 678 9012").address("Rua Mateus Sansão Muthemba, 96").billedKwh(2900.0).transformer(pt4).build()
        );
        // Soma Medidores = 17.600 kWh | Fornecido = 18.200 kWh | Perda = 600 kWh (3,30%) -> NORMAL
        pt4.setMeters(metersPt4);

        // =========================================================================
        // 5. POSTO DE TRANSFORMAÇÃO: "PT-AltoMae-02" (Bairro do Alto Maé, Maputo)
        // Setor em conformidade técnica (Perda ~3,53% <= 5,0%)
        // =========================================================================
        Transformer pt5 = Transformer.builder()
                .code("PT-AltoMae-02")
                .neighborhood("Alto Maé")
                .capacityKva(100.0)
                .totalSuppliedKwh(13600.0)
                .build();

        List<Meter> metersPt5 = Arrays.asList(
                Meter.builder().meterNumber("MED-MZ-5051").residentName("Inácio Tembe").phoneNumber("+258 84 890 1234").address("Av. Eduardo Mondlane, 840").billedKwh(2250.0).transformer(pt5).build(),
                Meter.builder().meterNumber("MED-MZ-5052").residentName("Judite Mondlane").phoneNumber("+258 82 901 2345").address("Av. Eduardo Mondlane, 856").billedKwh(2150.0).transformer(pt5).build(),
                Meter.builder().meterNumber("MED-MZ-5053").residentName("Leonardo Pelembe").phoneNumber("+258 87 012 3456").address("Rua dos Lusíadas, 45").billedKwh(2200.0).transformer(pt5).build(),
                Meter.builder().meterNumber("MED-MZ-5054").residentName("Marta Macamo").phoneNumber("+258 84 123 4560").address("Rua dos Lusíadas, 55").billedKwh(2100.0).transformer(pt5).build(),
                Meter.builder().meterNumber("MED-MZ-5055").residentName("Noémia Mabunda").phoneNumber("+258 82 234 5671").address("Av. 24 de Julho, 1420").billedKwh(2300.0).transformer(pt5).build(),
                Meter.builder().meterNumber("MED-MZ-5056").residentName("Orlando Zandamela").phoneNumber("+258 84 345 6782").address("Av. 24 de Julho, 1435").billedKwh(2120.0).transformer(pt5).build()
        );
        // Soma Medidores = 13.120 kWh | Fornecido = 13.600 kWh | Perda = 480 kWh (3,53%) -> NORMAL
        pt5.setMeters(metersPt5);

        // =========================================================================
        // 6. POSTO DE TRANSFORMAÇÃO: "PT-Malhangalene-03" (Bairro da Malhangalene, Maputo)
        // FRAUDE CRÍTICA: Perda de 34,71% > 5,0% (Suspeita de Ligações Clandestinas no Bairro)
        // =========================================================================
        Transformer pt6 = Transformer.builder()
                .code("PT-Malhangalene-03")
                .neighborhood("Malhangalene")
                .capacityKva(112.5)
                .totalSuppliedKwh(17000.0)
                .build();

        List<Meter> metersPt6 = Arrays.asList(
                Meter.builder().meterNumber("MED-MZ-6061").residentName("Pascoal Chissano").phoneNumber("+258 84 567 8903").address("Rua de Timor, 112").billedKwh(2100.0).transformer(pt6).build(),
                Meter.builder().meterNumber("MED-MZ-6062").residentName("Quitéria Nhantumbo").phoneNumber("+258 82 678 9014").address("Rua de Timor, 120").billedKwh(2000.0).transformer(pt6).build(),
                Meter.builder().meterNumber("MED-MZ-6063").residentName("Raul Guambe").phoneNumber("+258 87 789 0125").address("Av. Vladimir Lenine, 902").billedKwh(480.0).transformer(pt6).build(), // Bypass
                Meter.builder().meterNumber("MED-MZ-6064").residentName("Sara Magaia").phoneNumber("+258 84 890 1236").address("Av. Vladimir Lenine, 915").billedKwh(1950.0).transformer(pt6).build(),
                Meter.builder().meterNumber("MED-MZ-6065").residentName("Telmo Muchanga").phoneNumber("+258 82 901 2347").address("Rua da Resistência, 78").billedKwh(2050.0).transformer(pt6).build(),
                Meter.builder().meterNumber("MED-MZ-6066").residentName("Vânia Nhavoto").phoneNumber("+258 84 012 3458").address("Rua da Resistência, 86").billedKwh(510.0).transformer(pt6).build(), // Bypass
                Meter.builder().meterNumber("MED-MZ-6067").residentName("Wilson Muianga").phoneNumber("+258 87 123 4569").address("Av. Marien N'Gouabi, 504").billedKwh(2010.0).transformer(pt6).build()
        );
        // Soma Medidores = 11.100 kWh | Fornecido = 17.000 kWh | Perda = 5.900 kWh (34,71% > 20%) -> ALERTA CRÍTICO
        pt6.setMeters(metersPt6);

        transformerRepository.saveAll(Arrays.asList(pt1, pt2, pt3, pt4, pt5, pt6));
        log.info("Base de dados EDM populada com sucesso: 6 PTs e 41 medidores residenciais em Maputo.");
    }
}
