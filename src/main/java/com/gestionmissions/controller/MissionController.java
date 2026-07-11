package com.gestionmissions.controller;

import com.gestionmissions.service.MissionService;
import com.gestionmissions.util.JsonUtil;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class MissionController {
  private final MissionService missions; public MissionController(MissionService missions){this.missions=missions;}
  @GetMapping("/missions") public List<Map<String,Object>> all(){ return missions.missions(); }
  @PostMapping("/missions") public Map<String,Object> create(@RequestBody Map<String,Object> b){ return missions.create(b); }
  @PutMapping("/missions/{id}") public Map<String,Object> update(@PathVariable String id,@RequestBody Map<String,Object> b){ return missions.update(id,b); }
  @DeleteMapping("/missions/{id}") public Map<String,Object> delete(@PathVariable String id,@RequestParam(defaultValue="admin@system.local") String email){ missions.delete(id,email); return JsonUtil.ok("Mission supprimée"); }
  @GetMapping("/developers/{id}/recommendations") public List<Map<String,Object>> recs(@PathVariable String id){ return missions.recommendations(id); }
  @PostMapping("/missions/{id}/decision") public Map<String,Object> decision(@PathVariable String id,@RequestBody Map<String,Object> b){ missions.decision(id,b); return JsonUtil.ok("Décision enregistrée"); }
  @PostMapping("/missions/{id}/work") public Map<String,Object> deliver(@PathVariable String id,@RequestBody Map<String,Object> b){ missions.deliver(id,b); return JsonUtil.ok("Travail livré"); }
  @PostMapping("/missions/{id}/validation") public Map<String,Object> validate(@PathVariable String id,@RequestBody Map<String,Object> b){ missions.validate(id,b); return JsonUtil.ok("Validation enregistrée"); }
  @PostMapping("/missions/{id}/payment") public Map<String,Object> pay(@PathVariable String id){ missions.pay(id); return JsonUtil.ok("Paiement simulé"); }
  @PostMapping("/analytics/mission") public Map<String,Object> analyze(@RequestBody Map<String,Object> b){ return missions.analyze(String.valueOf(b.getOrDefault("title","")), String.valueOf(b.getOrDefault("description","")), JsonUtil.splitSkills(b.get("skills")), Double.parseDouble(String.valueOf(b.getOrDefault("budget",500)))); }
}
