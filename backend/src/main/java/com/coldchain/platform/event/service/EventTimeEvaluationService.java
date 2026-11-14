package com.coldchain.platform.event.service;

import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.shipment.model.Shipment;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class EventTimeEvaluationService {

    public enum EventTemporalDisposition {
        ACCEPTED_VALID,
        ACCEPTED_LATE,
        ACCEPTED_OUT_OF_ORDER,
        QUARANTINED_WATERMARK_BREACH,
        QUARANTINED_FUTURE_EVENT,
        QUARANTINED_BUSINESS_FINALIZED
    }

    /**
     * Evaluates whether an event can be accepted for event-time recalculation or must be quarantined.
     * Separates the allowed-lateness watermark from business finalization per specifications.
     */
    public EventTemporalDisposition evaluateTemporalDisposition(Shipment shipment,
                                                                 Instant eventTime,
                                                                 Instant ingestionTime,
                                                                 Instant lastObservedEventTime,
                                                                 boolean isIncidentFinalized) {
        // 1. Check explicit business finalization states (shipment CLOSED/DELIVERED, or incident DISPOSITIONED/CLOSED)
        if (shipment.isBusinessFinalized() || isIncidentFinalized) {
            return EventTemporalDisposition.QUARANTINED_BUSINESS_FINALIZED;
        }

        if (eventTime.isAfter(ingestionTime.plusSeconds(120))) {
            return EventTemporalDisposition.QUARANTINED_FUTURE_EVENT;
        }

        // 2. Check allowed-lateness watermark
        long allowedLatenessMinutes = shipment.getAllowedLatenessMinutes() != null
                ? shipment.getAllowedLatenessMinutes()
                : 120L;

        long latenessMinutes = Duration.between(eventTime, ingestionTime).toMinutes();
        if (latenessMinutes > allowedLatenessMinutes) {
            return EventTemporalDisposition.QUARANTINED_WATERMARK_BREACH;
        }

        // 3. Inside watermark window: check if out of order or late
        if (lastObservedEventTime != null && eventTime.isBefore(lastObservedEventTime)) {
            return EventTemporalDisposition.ACCEPTED_OUT_OF_ORDER;
        }

        if (latenessMinutes > 15) { // Arrived later than typical pipeline latency
            return EventTemporalDisposition.ACCEPTED_LATE;
        }

        return EventTemporalDisposition.ACCEPTED_VALID;
    }

    public EventQualityFlag toQualityFlag(EventTemporalDisposition disposition) {
        return switch (disposition) {
            case ACCEPTED_VALID -> EventQualityFlag.VALID;
            case ACCEPTED_LATE -> EventQualityFlag.LATE;
            case ACCEPTED_OUT_OF_ORDER -> EventQualityFlag.OUT_OF_ORDER;
            case QUARANTINED_WATERMARK_BREACH, QUARANTINED_FUTURE_EVENT,
                    QUARANTINED_BUSINESS_FINALIZED -> EventQualityFlag.QUARANTINED;
        };
    }
}
