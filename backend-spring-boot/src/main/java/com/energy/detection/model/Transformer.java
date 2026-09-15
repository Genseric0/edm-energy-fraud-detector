package com.energy.detection.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um Transformador de Distribuição.
 * Monitora o total de energia fornecida (injetada na rede daquele setor).
 */
@Entity
@Table(name = "transformers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transformer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Ex: "TR-SP-101"

    @Column(nullable = false)
    private String neighborhood; // Bairro / Setor (Ex: "Jardim Paulista")

    @Column(name = "capacity_kva")
    private Double capacityKva; // Capacidade nominal em kVA (Ex: 75.0 kVA)

    @Column(name = "total_supplied_kwh", nullable = false)
    private Double totalSuppliedKwh; // Energia total fornecida/medida no secundário do transformador

    @OneToMany(mappedBy = "transformer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<Meter> meters = new ArrayList<>();

    // Método utilitário para adicionar medidor
    public void addMeter(Meter meter) {
        meters.add(meter);
        meter.setTransformer(this);
    }

    // Método utilitário para remover medidor
    public void removeMeter(Meter meter) {
        meters.remove(meter);
        meter.setTransformer(null);
    }
}
