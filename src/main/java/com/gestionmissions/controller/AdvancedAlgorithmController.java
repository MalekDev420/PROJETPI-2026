package com.gestionmissions.controller;

import com.gestionmissions.service.AdvancedAlgorithmService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/advanced")
public class AdvancedAlgorithmController {
  private final AdvancedAlgorithmService advanced;
  public AdvancedAlgorithmController(AdvancedAlgorithmService advanced) { this.advanced = advanced; }

  @PostMapping("/analyze-mission") public Map<String,Object> analyzeMission(@RequestBody Map<String,Object> body) { return advanced.analyzeMission(body); }
  @PostMapping("/top-developers") public List<Map<String,Object>> topDevelopers(@RequestBody Map<String,Object> body) { return advanced.topDevelopersForMission(body); }
  @PostMapping("/predict-success") public Map<String,Object> predict(@RequestBody Map<String,Object> body) { return advanced.predict(body); }
  @PostMapping("/compare-developers") public List<Map<String,Object>> compare(@RequestBody Map<String,Object> body) { return advanced.compareDevelopers(body); }
  @GetMapping("/scan-anomalies") public List<Map<String,Object>> anomalies() { return advanced.scanAnomalies(); }
  @PostMapping("/optimize-budget") public Map<String,Object> optimizeBudget(@RequestBody Map<String,Object> body) { return advanced.optimizeBudget(body); }
  @GetMapping("/predictive-dashboard") public Map<String,Object> predictiveDashboard() { return advanced.predictiveDashboard(); }
  @PostMapping("/explain-matching") public Map<String,Object> explain(@RequestBody Map<String,Object> body) { return advanced.explainMatching(body); }
  @PostMapping("/generate-spec") public Map<String,Object> generateSpec(@RequestBody Map<String,Object> body) { return advanced.generateSpec(body); }
  @PostMapping("/improve-description") public Map<String,Object> improve(@RequestBody Map<String,Object> body) { return advanced.improveDescription(body); }
  @PostMapping("/recommend-best-developer") public Map<String,Object> recommendBest(@RequestBody Map<String,Object> body) { return advanced.recommendBestDeveloper(body); }
  @PostMapping("/publish-optimized-mission") public Map<String,Object> publishOptimized(@RequestBody Map<String,Object> body) { return advanced.publishOptimizedMission(body); }
  @PostMapping("/deadline-plan") public Map<String,Object> deadlinePlan(@RequestBody Map<String,Object> body) { return advanced.deadlinePlan(body); }
  @PostMapping("/roi-estimate") public Map<String,Object> roiEstimate(@RequestBody Map<String,Object> body) { return advanced.roiEstimate(body); }
  @PostMapping("/quality-checklist") public Map<String,Object> qualityChecklist(@RequestBody Map<String,Object> body) { return advanced.qualityChecklist(body); }
  @PostMapping("/market-benchmark") public Map<String,Object> marketBenchmark(@RequestBody Map<String,Object> body) { return advanced.marketBenchmark(body); }
  @PostMapping("/mission-health") public Map<String,Object> missionHealth(@RequestBody Map<String,Object> body) { return advanced.missionHealth(body); }
  @PostMapping("/milestone-plan") public Map<String,Object> milestonePlan(@RequestBody Map<String,Object> body) { return advanced.milestonePlan(body); }
  @PostMapping("/tech-stack-advice") public Map<String,Object> techStackAdvice(@RequestBody Map<String,Object> body) { return advanced.techStackAdvice(body); }
  @PostMapping("/negotiation-strategy") public Map<String,Object> negotiationStrategy(@RequestBody Map<String,Object> body) { return advanced.negotiationStrategy(body); }
}

