package com.energy.detection.model;

/** Localização verificada do contador; não implica detecção automática de fraude. */
public enum MeterLocation {
    POLE,
    INSIDE_HOME,
    OUTSIDE_HOME,
    UNKNOWN
}
