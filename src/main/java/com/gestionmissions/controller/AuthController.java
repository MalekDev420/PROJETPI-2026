package com.gestionmissions.controller;

import com.gestionmissions.service.UserService;
import com.gestionmissions.util.JsonUtil;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class AuthController {
  private final UserService users; public AuthController(UserService users){this.users=users;}
  @PostMapping("/auth/client") public Map<String,Object> client(@RequestBody Map<String,Object> b){ return users.auth("client", b); }
  @PostMapping("/auth/developer") public Map<String,Object> developer(@RequestBody Map<String,Object> b){ return users.auth("developer", b); }
  @PostMapping("/register/client") public Map<String,Object> registerClient(@RequestBody Map<String,Object> b){ return users.registerClient(b); }
  @PostMapping("/register/developer") public Map<String,Object> registerDev(@RequestBody Map<String,Object> b){ return users.createDeveloper(b); }
  @PostMapping("/auth/forgot/client") public Map<String,Object> forgotClient(@RequestBody Map<String,Object> b){ users.forgot("client", String.valueOf(b.get("email"))); return JsonUtil.ok("Nouveau mot de passe envoyé au client."); }
  @PostMapping("/auth/forgot/developer") public Map<String,Object> forgotDev(@RequestBody Map<String,Object> b){ users.forgot("developer", String.valueOf(b.get("email"))); return JsonUtil.ok("Nouveau mot de passe envoyé au développeur."); }
}
