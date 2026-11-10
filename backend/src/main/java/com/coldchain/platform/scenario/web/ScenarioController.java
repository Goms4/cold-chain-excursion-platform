package com.coldchain.platform.scenario.web;

import com.coldchain.platform.scenario.dto.ScenarioRunResult;
import com.coldchain.platform.scenario.service.ScenarioRunnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioRunnerService scenarioRunnerService;

    public ScenarioController(ScenarioRunnerService scenarioRunnerService) {
        this.scenarioRunnerService = scenarioRunnerService;
    }

    @PostMapping("/run/{name}")
    public ResponseEntity<ScenarioRunResult> runScenario(@PathVariable String name) {
        return ResponseEntity.ok(scenarioRunnerService.runScenario(name));
    }

    @PostMapping("/run-all")
    public ResponseEntity<ScenarioRunResult> runAllScenarios() {
        return ResponseEntity.ok(scenarioRunnerService.runAllScenarios());
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, String>>> listScenarios() {
        return ResponseEntity.ok(List.of(
                Map.of("id", "normal", "title", "1. Normal Shipment", "description", "Compliant temperatures (18-22°C), zero incidents."),
                Map.of("id", "minor", "title", "2. Minor Excursion", "description", "26-28.2°C excursion in room-temperature profile."),
                Map.of("id", "critical", "title", "3. Major / Critical Excursion", "description", "Extreme heat (>40°C), critical severity."),
                Map.of("id", "duplicate", "title", "4. Duplicate Event", "description", "Duplicate event_id suppressed; zero duplicate incidents."),
                Map.of("id", "out-of-order", "title", "5. Out-of-Order & Late Event", "description", "Events 14:00, 14:10, 14:05 evaluated by event_time."),
                Map.of("id", "missing-contradictory", "title", "6. Missing Interval & Contradictory Inspection", "description", "20m telemetry gap marked UNKNOWN. Sensor 11.8°C vs manual inspection 6.2°C.")
        ));
    }
}
