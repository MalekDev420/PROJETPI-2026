package com.gestionmissions.controller;

import com.gestionmissions.service.DashboardService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class SystemController {
  private final DashboardService dashboard; public SystemController(DashboardService dashboard){this.dashboard=dashboard;}
  @GetMapping("/stats") public Map<String,Object> stats(){ return dashboard.stats(); }
  @GetMapping("/analytics/dashboard") public Map<String,Object> analytics(){ return dashboard.stats(); }
  @GetMapping("/emails") public List<Map<String,Object>> emails(){ return dashboard.emails(); }
  @GetMapping("/notifications") public List<Map<String,Object>> notifications(@RequestParam(required=false) String to){ return dashboard.notifications(to); }
  @GetMapping("/workflow") public List<Map<String,Object>> workflow(){ return dashboard.workflow(); }
}
