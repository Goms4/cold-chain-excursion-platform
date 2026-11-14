package com.coldchain.platform.event.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.event.model.MissingInterval;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.shipment.model.Shipment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class MissingDataDetectorService {

    private final MissingIntervalRepository missingIntervalRepository;
    private final AuditService auditService;

    public MissingDataDetectorService(MissingIntervalRepository missingIntervalRepository,
                                      AuditService auditService) {
        this.missingIntervalRepository = missingIntervalRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Optional<MissingInterval> checkAndRecordMissingInterval(Shipment shipment,
                                                                  Sensor sensor,
                                                                  Instant previousEventTime,
                                                                  Instant currentEventTime) {
        if (previousEventTime == null || currentEventTime == null) {
            return Optional.empty();
        }

        int cadenceMinutes = shipment.getExpectedCadenceMinutes() != null
                ? shipment.getExpectedCadenceMinutes()
                : 5;

        // If current event arrived later than expected cadence + small 1-minute jitter allowance
        long gapMinutes = Duration.between(previousEventTime, currentEventTime).toMinutes();
        if (gapMinutes > cadenceMinutes + 1) {
            MissingInterval missingInterval = new MissingInterval(
                    shipment,
                    sensor,
                    previousEventTime,
                    currentEventTime,
                    cadenceMinutes
            );

            MissingInterval saved = missingIntervalRepository.save(missingInterval);

            auditService.recordAudit("MISSING_DATA", String.valueOf(saved.getId()), "GAP_DETECTED", "System",
                    null, "Missing interval from " + previousEventTime + " to " + currentEventTime + " marked UNKNOWN",
                    "Cadence breach: expected " + cadenceMinutes + "m, actual gap " + gapMinutes + "m. Status: UNKNOWN (never SAFE).");

            return Optional.of(saved);
        }

        return Optional.empty();
    }
}
