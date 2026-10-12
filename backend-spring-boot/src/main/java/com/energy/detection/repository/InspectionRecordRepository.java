package com.energy.detection.repository;

import com.energy.detection.model.InspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {
    List<InspectionRecord> findTop100ByMeterIdOrderByCreatedAtDesc(Long meterId);
}
