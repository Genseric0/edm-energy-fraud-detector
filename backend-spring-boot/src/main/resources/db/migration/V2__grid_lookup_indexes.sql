-- Índices para junções por FK e históricos ordenados, sem alterar os registos.
CREATE INDEX idx_meter_transformer ON residential_meters(transformer_id);
CREATE INDEX idx_reading_meter_time ON meter_readings(meter_id, recorded_at);
CREATE INDEX idx_inspection_meter_time ON inspection_records(meter_id, created_at);
