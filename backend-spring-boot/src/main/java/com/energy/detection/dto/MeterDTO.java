package com.energy.detection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para trânsito de dados de medidores residenciais.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterDTO {
    private Long id;
    private String meterNumber;
    private String residentName;
    private String phoneNumber;
    private String address;
    private Double billedKwh;
    private String installationLocation;
    private boolean remoteReadingEnabled;
    private Long transformerId;
    private String transformerCode;
}
