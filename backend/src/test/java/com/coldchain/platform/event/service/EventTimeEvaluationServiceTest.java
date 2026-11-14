package com.coldchain.platform.event.service;

import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTimeEvaluationServiceTest {
    private final EventTimeEvaluationService service = new EventTimeEvaluationService();

    @Test
    void futureEventsBeyondTwoMinuteToleranceAreQuarantined() {
        Instant now = Instant.now();
        assertEquals(EventTimeEvaluationService.EventTemporalDisposition.QUARANTINED_FUTURE_EVENT,
                service.evaluateTemporalDisposition(shipment(), now.plus(3, ChronoUnit.MINUTES), now, null, false));
    }

    @Test
    void outOfOrderEventsInsideTheWatermarkRemainAccepted() {
        Instant now = Instant.now();
        assertEquals(EventTimeEvaluationService.EventTemporalDisposition.ACCEPTED_OUT_OF_ORDER,
                service.evaluateTemporalDisposition(shipment(), now.minus(2, ChronoUnit.MINUTES), now,
                        now.minus(1, ChronoUnit.MINUTES), false));
    }

    private Shipment shipment() {
        StorageProfile profile = new StorageProfile("PROFILE-REF", "Refrigerated",
                new BigDecimal("2"), new BigDecimal("8"), new BigDecimal("8.1"), new BigDecimal("12"),
                new BigDecimal("12.1"), new BigDecimal("20"), new BigDecimal("20.1"), new BigDecimal("99.9"), 120, "v1.0.0");
        return new Shipment("SHP-10001", "TRK-1", "Origin", "Destination", profile,
                ShipmentStatus.IN_TRANSIT, 5, 120);
    }
}
