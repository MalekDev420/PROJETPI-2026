package com.gestionmissions.controller;

import com.gestionmissions.service.AssistantService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/assistant")
public class AssistantController {
  private final AssistantService assistant; public AssistantController(AssistantService assistant){this.assistant=assistant;}
  @GetMapping("/status") public Map<String,Object> status(){ return assistant.status(); }
  @PostMapping("/chat") public Map<String,Object> chat(@RequestBody Map<String,Object> b){ return Map.of("reply", assistant.chat(String.valueOf(b.getOrDefault("message","")))); }
}
