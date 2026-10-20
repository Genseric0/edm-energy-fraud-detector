-- Falhar sem alterar dados se houver registos que exijam reconciliação humana.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM meter_readings
        WHERE energy_kwh IN ('NaN'::double precision, 'Infinity'::double precision, '-Infinity'::double precision)
           OR energy_kwh < 0 OR energy_kwh > 99999999999.999) THEN
        RAISE EXCEPTION 'Reading migration blocked: invalid or out-of-range energy values';
    END IF;
    IF EXISTS (SELECT 1 FROM meter_readings
        WHERE energy_kwh::text::numeric <> round(energy_kwh::text::numeric, 3)) THEN
        RAISE EXCEPTION 'Reading migration blocked: reconcile precision before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM meter_readings GROUP BY meter_id, recorded_at, source HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Reading migration blocked: reconcile duplicate readings before migration';
    END IF;
END;
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM inspection_records
        WHERE estimated_unaccounted_kwh IN ('NaN'::double precision, 'Infinity'::double precision, '-Infinity'::double precision)
           OR estimated_unaccounted_kwh < 0 OR estimated_unaccounted_kwh > 99999999999.999) THEN
        RAISE EXCEPTION 'Inspection migration blocked: invalid or out-of-range estimates';
    END IF;
    IF EXISTS (SELECT 1 FROM inspection_records
        WHERE estimated_unaccounted_kwh::text::numeric <> round(estimated_unaccounted_kwh::text::numeric, 3)) THEN
        RAISE EXCEPTION 'Inspection migration blocked: reconcile precision before migration';
    END IF;
END;
$$;

ALTER TABLE meter_readings ALTER COLUMN energy_kwh TYPE NUMERIC(14,3) USING energy_kwh::text::numeric;
ALTER TABLE meter_readings ADD CONSTRAINT ck_meter_reading_energy_range
    CHECK (energy_kwh >= 0 AND energy_kwh <= 99999999999.999);
ALTER TABLE meter_readings ADD CONSTRAINT uk_meter_reading_period_source UNIQUE (meter_id, recorded_at, source);
ALTER TABLE meter_readings ADD COLUMN ingested_by VARCHAR(100);
ALTER TABLE inspection_records ALTER COLUMN estimated_unaccounted_kwh TYPE NUMERIC(14,3) USING estimated_unaccounted_kwh::text::numeric;
ALTER TABLE inspection_records ADD CONSTRAINT ck_inspection_estimate_range
    CHECK (estimated_unaccounted_kwh >= 0 AND estimated_unaccounted_kwh <= 99999999999.999);
-- Legado permanece NULL: não atribuir a um utilizador que não foi confirmado.
