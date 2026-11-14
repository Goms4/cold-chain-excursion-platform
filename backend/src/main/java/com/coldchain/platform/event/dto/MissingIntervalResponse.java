package com.coldchain.platform.event.dto;

import com.coldchain.platform.common.BusinessIds;
import com.coldchain.platform.event.model.MissingInterval;
import java.time.Instant;

public record MissingIntervalResponse(String intervalId, String shipmentId, String sensorId,
                                      Instant gapStartTime, Instant gapEndTime,
                                      String condition, String dataQuality) {
    public static MissingIntervalResponse from(MissingInterval interval) {
        return new MissingIntervalResponse("MISS-%06d".formatted(interval.getId()),
                BusinessIds.shipment(interval.getShipment().getId()),
                BusinessIds.sensor(interval.getSensor().getId()), interval.getGapStartTime(),
                interval.getGapEndTime(), "UNKNOWN", "MISSING");
    }
}
