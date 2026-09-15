package com.energy.detection.repository;

import com.energy.detection.model.Transformer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformerRepository extends JpaRepository<Transformer, Long> {

    Optional<Transformer> findByCode(String code);

    List<Transformer> findByNeighborhoodContainingIgnoreCase(String neighborhood);

    // Carrega transformadores com seus medidores (evitando N+1)
    @Query("SELECT DISTINCT t FROM Transformer t LEFT JOIN FETCH t.meters")
    List<Transformer> findAllWithMeters();

    @Query("SELECT t FROM Transformer t LEFT JOIN FETCH t.meters WHERE t.id = :id")
    Optional<Transformer> findByIdWithMeters(Long id);
}
