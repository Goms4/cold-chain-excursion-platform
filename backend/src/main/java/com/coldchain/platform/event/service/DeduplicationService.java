package com.coldchain.platform.event.service;

import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import org.springframework.stereotype.Service;

@Service
public class DeduplicationService {

    private final ProcessedSensorEventRepository processedSensorEventRepository;

    public DeduplicationService(ProcessedSensorEventRepository processedSensorEventRepository) {
        this.processedSensorEventRepository = processedSensorEventRepository;
    }

    public boolean isDuplicateEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return processedSensorEventRepository.existsByEventId(eventId);
    }
}
