package com.energy.detection.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade que representa um Medidor Residencial de Energia Elétrica.
 * Armazena o consumo aferido e faturado na residência do cliente.
 */
@Entity
@Table(name = "residential_meters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_number", nullable = false, unique = true)
    private String meterNumber; // Ex: "MED-98421"

    @Column(name = "resident_name", nullable = false)
    private String residentName; // Ex: "Alberto Cossa"

    @Column(name = "phone_number")
    private String phoneNumber; // Ex: "+258 84 312 4567"

    @Column(nullable = false)
    private String address; // Ex: "Av. de Moçambique, 405"

    @Column(name = "billed_kwh", nullable = false)
    private Double billedKwh; // Energia consumida e faturada pelo medidor (kWh)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id", nullable = false)
    @JsonBackReference
    private Transformer transformer;
}
