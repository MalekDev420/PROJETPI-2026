package com.gestionmissions.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DashboardService {
  private final JdbcTemplate db; private final MissionService missions;
  public DashboardService(JdbcTemplate db, MissionService missions){this.db=db;this.missions=missions;}
  public Map<String,Object> stats(){
    Map<String,Object> s=new LinkedHashMap<>();
    s.put("missions", db.queryForObject("SELECT COUNT(*) FROM missions", Integer.class));
    s.put("developers", db.queryForObject("SELECT COUNT(*) FROM users WHERE role='developer'", Integer.class));
    s.put("clients", db.queryForObject("SELECT COUNT(*) FROM users WHERE role='client'", Integer.class));
    s.put("budget", db.queryForObject("SELECT COALESCE(SUM(budget),0) FROM missions", Double.class));
    s.put("avgRating", db.queryForObject("SELECT ROUND(COALESCE(AVG(rating),0),2) FROM users WHERE role='developer'", Double.class));
    s.put("avgRisk", db.queryForObject("SELECT ROUND(COALESCE(AVG(risk_score),0),2) FROM missions", Double.class));
    s.put("avgComplexity", db.queryForObject("SELECT ROUND(COALESCE(AVG(complexity_score),0),2) FROM missions", Double.class));
    s.put("successRate", db.queryForObject("SELECT ROUND(100*SUM(status IN ('validated','paid'))/GREATEST(COUNT(*),1),2) FROM missions", Double.class));
    s.put("topTechnologies", missions.topTechnologies());
    s.put("riskyMissions", db.queryForList("SELECT title,risk_score riskScore,complexity_label complexityLabel,budget FROM missions ORDER BY risk_score DESC LIMIT 5"));
    return s;
  }
  public List<Map<String,Object>> emails(){ return db.queryForList("SELECT recipient_email `to`,subject,body,status,created_at createdAt FROM email_logs ORDER BY created_at DESC LIMIT 30"); }
  public List<Map<String,Object>> notifications(String to){ if(to==null||to.isBlank()) return db.queryForList("SELECT recipient_email `to`,message,type,created_at createdAt FROM notifications ORDER BY created_at DESC LIMIT 50"); return db.queryForList("SELECT recipient_email `to`,message,type,created_at createdAt FROM notifications WHERE recipient_email=? ORDER BY created_at DESC LIMIT 50", to); }
  public List<Map<String,Object>> workflow(){ return db.queryForList("SELECT message,created_at createdAt FROM workflow_events ORDER BY created_at DESC LIMIT 50"); }
}
