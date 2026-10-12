package com.energy.detection.repository;

import com.energy.detection.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    List<MeterReading> findTop100ByMeterIdOrderByRecordedAtDesc(Long meterId);
}
