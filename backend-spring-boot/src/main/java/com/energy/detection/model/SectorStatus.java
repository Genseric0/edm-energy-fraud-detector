package com.energy.detection.model;

/**
 * Status de classificação do setor quanto à perda não técnica.
 */
public enum SectorStatus {
    NORMAL("Normal: Perda Técnica Aceitável"),
    CRITICAL_ALERT("Alerta Crítico: Suspeita de Ligação Clandestina");

    private final String description;

    SectorStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
