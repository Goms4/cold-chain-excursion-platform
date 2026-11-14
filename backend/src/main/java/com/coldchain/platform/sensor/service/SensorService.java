package com.coldchain.platform.sensor.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.BusinessIdGenerator;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.sensor.dto.AssignSensorRequest;
import com.coldchain.platform.sensor.dto.RegisterSensorRequest;
import com.coldchain.platform.sensor.dto.SensorResponse;
import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.sensor.repository.SensorRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SensorService {

    private final SensorRepository sensorRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuditService auditService;
    private final BusinessIdGenerator businessIdGenerator;

    public SensorService(SensorRepository sensorRepository,
                         ShipmentRepository shipmentRepository,
                         AuditService auditService,
                         BusinessIdGenerator businessIdGenerator) {
        this.sensorRepository = sensorRepository;
        this.shipmentRepository = shipmentRepository;
        this.auditService = auditService;
        this.businessIdGenerator = businessIdGenerator;
    }

    @Transactional
    public SensorResponse provisionForShipment(Shipment shipment, String actor) {
        Sensor sensor = new Sensor(businessIdGenerator.nextSensorId(), "ColdLatch temperature logger", 100, shipment);
        Sensor saved = sensorRepository.save(sensor);
        auditService.recordAudit("SENSOR", saved.getId(), "REGISTERED", actor, null,
                "Provisioned temperature logger " + saved.getId() + " for shipment " + shipment.getId(), "Shipment registration");
        return SensorResponse.fromEntity(saved);
    }

    @Transactional
    public SensorResponse registerSensor(RegisterSensorRequest request, String actor) {
        if (sensorRepository.existsById(request.getId())) {
            throw new ValidationException("Sensor with ID '" + request.getId() + "' already exists.");
        }

        Shipment shipment = null;
        if (request.getShipmentId() != null && !request.getShipmentId().isBlank()) {
            shipment = shipmentRepository.findById(request.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment", request.getShipmentId()));
        }

        Sensor sensor = new Sensor(request.getId(), request.getModel(), request.getBatteryLevel(), shipment);
        Sensor saved = sensorRepository.save(sensor);

        auditService.recordAudit("SENSOR", saved.getId(), "REGISTERED", actor, null,
                "Registered sensor " + saved.getId(), "Sensor registration");

        return SensorResponse.fromEntity(saved);
    }

    @Transactional
    public SensorResponse assignSensorToShipment(String shipmentId, AssignSensorRequest request, String actor) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));

        Sensor sensor = sensorRepository.findById(request.getSensorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", request.getSensorId()));

        String oldShipmentId = sensor.getShipment() != null ? sensor.getShipment().getId() : "NONE";
        sensor.setShipment(shipment);
        Sensor saved = sensorRepository.save(sensor);

        auditService.recordAudit("SENSOR", saved.getId(), "ASSIGNED", actor,
                "shipment: " + oldShipmentId, "shipment: " + shipmentId, "Sensor assignment to shipment");

        return SensorResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Sensor getSensorEntity(String id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", id));
    }

    @Transactional(readOnly = true)
    public SensorResponse getSensor(String id) {
        return SensorResponse.fromEntity(getSensorEntity(id));
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> getAllSensors() {
        return sensorRepository.findAll().stream()
                .map(SensorResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SensorResponse> getSensorsForShipment(String shipmentId) {
        return sensorRepository.findByShipmentId(shipmentId).stream()
                .map(SensorResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
