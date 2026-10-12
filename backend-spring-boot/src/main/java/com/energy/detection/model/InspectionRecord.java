package com.energy.detection.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Registo preliminar de vistoria; não constitui prova de fraude nem auto de multa. */
@Entity
@Table(name = "inspection_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InspectionRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reported_by", nullable = false, updatable = false)
    private String reportedBy;

    @Column(name = "estimated_unaccounted_kwh", nullable = false)
    private Double estimatedUnaccountedKwh;

    @Column(name = "inspection_notes", length = 4000)
    private String inspectionNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status { PENDING_REVIEW, UNDER_INSPECTION, INCONCLUSIVE, DISCARDED, VERIFIED_TECHNICAL_IRREGULARITY }
}
