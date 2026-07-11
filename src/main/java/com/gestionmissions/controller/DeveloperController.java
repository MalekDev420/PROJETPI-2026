package com.gestionmissions.controller;

import com.gestionmissions.service.UserService;
import com.gestionmissions.util.JsonUtil;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class DeveloperController {
  private final UserService users; public DeveloperController(UserService users){this.users=users;}
  @GetMapping("/developers") public List<Map<String,Object>> all(){ return users.developers(); }
  @GetMapping("/developers/{id}") public Map<String,Object> one(@PathVariable String id){ return users.developer(id); }
  @PostMapping("/admin/developers") public Map<String,Object> create(@RequestBody Map<String,Object> b){ return users.createDeveloper(b); }
  @DeleteMapping("/admin/developers/{id}") public Map<String,Object> delete(@PathVariable String id){ users.deleteDeveloper(id); return JsonUtil.ok("Développeur supprimé"); }
  @PutMapping("/developers/{id}/profile") public Map<String,Object> update(@PathVariable String id,@RequestBody Map<String,Object> b){ return users.updateDeveloper(id,b); }
  @PostMapping("/developers/{id}/reviews") public Map<String,Object> rate(@PathVariable String id,@RequestBody Map<String,Object> b){ return users.rate(id,b); }
}
