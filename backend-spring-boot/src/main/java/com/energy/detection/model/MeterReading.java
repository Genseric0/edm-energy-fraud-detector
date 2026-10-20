package com.energy.detection.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;

/** Leitura histórica de consumo, independente da localização física do contador. */
@Entity
@Table(name = "meter_readings", indexes = @Index(name = "idx_reading_meter_time", columnList = "meter_id,recorded_at"),
    uniqueConstraints = @UniqueConstraint(name = "uk_meter_reading_period_source", columnNames = {"meter_id", "recorded_at", "source"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeterReading {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "energy_kwh", nullable = false, precision = 14, scale = 3)
    private BigDecimal energyKwh;

    @Column(name = "source", nullable = false)
    private String source;

    // Registos anteriores à V6 não têm autor confirmado; não inventar proveniência.
    @Column(name = "ingested_by", length = 100)
    private String ingestedBy;
}
