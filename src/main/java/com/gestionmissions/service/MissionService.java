package com.gestionmissions.service;

import com.gestionmissions.util.JsonUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MissionService {
  private final JdbcTemplate db; private final DataScienceService data; private final NotificationService notify;
  public MissionService(JdbcTemplate db, DataScienceService data, NotificationService notify){this.db=db;this.data=data;this.notify=notify;}

  public List<Map<String,Object>> missions(){ return db.queryForList("SELECT * FROM missions ORDER BY created_at DESC").stream().map(this::mapMission).toList(); }
  public Map<String,Object> mission(String id){ var rows=db.queryForList("SELECT * FROM missions WHERE id=?", id); if(rows.isEmpty()) throw new RuntimeException("Mission introuvable"); return mapMission(rows.get(0)); }
  public Map<String,Object> create(Map<String,Object> b){
    String id=JsonUtil.id("m"); String title=val(b,"title","Mission générée"); String description=val(b,"description",""); List<String> skills=JsonUtil.splitSkills(b.get("skills"));
    if(skills.isEmpty()) skills = data.extractSkills(title+" "+description); double budget=num(b,"budget",500);
    Map<String,Object> a = analyze(title, description, skills, budget); String owner=val(b,"ownerEmail","client@demo.com").toLowerCase(); String clientId=findClientId(owner);
    db.update("INSERT INTO missions(id,title,description,cleaned_description,category,extracted_skills,complexity_score,complexity_label,risk_score,estimated_budget_min,estimated_budget_max,estimated_days,budget,skills,status,owner_email,client_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
      id,title,description,a.get("cleanedDescription"),a.get("category"),JsonUtil.csv(a.get("extractedSkills")),a.get("complexityScore"),a.get("complexityLabel"),a.get("riskScore"),a.get("estimatedBudgetMin"),a.get("estimatedBudgetMax"),a.get("estimatedDays"),budget,JsonUtil.csv(skills),"open",owner,clientId);
    notify.workflow("Mission créée avec nettoyage NLP et analyse algorithmique: "+title); return mission(id);
  }
  public Map<String,Object> update(String id, Map<String,Object> b){
    Map<String,Object> old = mission(id); String title=val(b,"title",String.valueOf(old.get("title"))); String description=val(b,"description",String.valueOf(old.get("description"))); List<String> skills=b.containsKey("skills")?JsonUtil.splitSkills(b.get("skills")):(List<String>)old.get("skills"); double budget=num(b,"budget",Double.parseDouble(String.valueOf(old.get("budget")))); Map<String,Object> a=analyze(title,description,skills,budget);
    db.update("UPDATE missions SET title=?,description=?,cleaned_description=?,category=?,extracted_skills=?,complexity_score=?,complexity_label=?,risk_score=?,estimated_budget_min=?,estimated_budget_max=?,estimated_days=?,budget=?,skills=?,status=COALESCE(?,status),owner_email=COALESCE(?,owner_email) WHERE id=?",
      title,description,a.get("cleanedDescription"),a.get("category"),JsonUtil.csv(a.get("extractedSkills")),a.get("complexityScore"),a.get("complexityLabel"),a.get("riskScore"),a.get("estimatedBudgetMin"),a.get("estimatedBudgetMax"),a.get("estimatedDays"),budget,JsonUtil.csv(skills),b.get("status"),b.get("ownerEmail"),id);
    return mission(id);
  }
  public void delete(String id, String email){
    var m=mission(id); if(!"admin@system.local".equalsIgnoreCase(email) && !String.valueOf(m.get("ownerEmail")).equalsIgnoreCase(email)) throw new RuntimeException("Suppression refusée: seul le créateur peut supprimer sa mission");
    db.update("DELETE FROM missions WHERE id=?", id); notify.workflow("Mission supprimée: "+m.get("title"));
  }
  public List<Map<String,Object>> recommendations(String devId){
    var devRows=db.queryForList("SELECT * FROM users WHERE id=? AND role='developer'", devId); if(devRows.isEmpty()) return List.of(); Map<String,Object> d=devRows.get(0); List<String> dskills=JsonUtil.splitSkills(d.get("skills")); double reputation=num(d,"reputation",50); String availability=String.valueOf(d.get("availability")); int completed=(int)num(d,"missions_completed",0); int refusals=(int)num(d,"refusals",0);
    return db.queryForList("SELECT * FROM missions WHERE status='open' ORDER BY created_at DESC").stream().map(r->{
      Map<String,Object> m=mapMission(r); List<String> mskills=(List<String>)m.get("skills"); double sim=data.cosine(dskills, mskills); double domain = String.valueOf(d.get("domain")).equalsIgnoreCase(String.valueOf(m.get("category"))) ? 100 : 55;
      double risk=num(m,"riskScore",0); double score = data.clamp(sim*100*0.42 + reputation*0.22 + domain*0.16 + ("busy".equals(availability)?45:90)*0.10 + completed*2 - refusals*2 - risk*0.08,0,100);
      double success=data.predictionSuccess(score,reputation,availability,completed,refusals,risk);
      List<String> reasons=new ArrayList<>(); reasons.add("Similarité cosinus compétences: "+Math.round(sim*100)+"%"); reasons.add("Réputation développeur: "+Math.round(reputation)+"/100"); reasons.add("Catégorie détectée: "+m.get("category")); reasons.add("Risque mission calculé: "+Math.round(risk)+"%"); reasons.add("Probabilité réussite prédite: "+Math.round(success)+"%");
      Map<String,Object> res=new LinkedHashMap<>(); res.put("mission",m); res.put("score",Math.round(score)); res.put("successPrediction",Math.round(success)); res.put("reasons",reasons); return res;
    }).sorted((a,b)->Long.compare((Long)b.get("score"),(Long)a.get("score"))).toList();
  }
  public void decision(String missionId, Map<String,Object> b){
    String devId=val(b,"developerId",""); String decision=val(b,"decision","refuse"); Map<String,Object> m=mission(missionId); var dev=db.queryForMap("SELECT * FROM users WHERE id=?", devId); String client=String.valueOf(m.get("ownerEmail")); String devEmail=String.valueOf(dev.get("email"));
    if("accept".equals(decision)) { db.update("UPDATE missions SET status='accepted_by_developer', assigned_developer_id=? WHERE id=?", devId, missionId); sendBoth(client,devEmail,"Mission acceptée","Le développeur "+dev.get("name")+" a accepté la mission: "+m.get("title")); }
    else { db.update("UPDATE missions SET status='refused_by_developer' WHERE id=?", missionId); db.update("UPDATE users SET refusals=refusals+1 WHERE id=?", devId); sendBoth(client,devEmail,"Mission refusée","Le développeur "+dev.get("name")+" a refusé la mission: "+m.get("title")); }
    notify.workflow("Décision développeur: "+decision+" sur "+m.get("title"));
  }
  public void deliver(String missionId, Map<String,Object> b){
    String devId=val(b,"developerId",""); String desc=val(b,"description",""); String link=val(b,"link",""); Map<String,Object> m=mission(missionId); var dev=db.queryForMap("SELECT * FROM users WHERE id=?", devId);
    double quality = data.clamp(50 + (link.contains("github")||link.contains("drive")?20:5) + Math.min(desc.length()/8.0,25),0,100);
    db.update("INSERT INTO deliveries(id,mission_id,developer_id,description,link,quality_score,status) VALUES(?,?,?,?,?,?,?)", JsonUtil.id("del"), missionId, devId, desc, link, quality, "delivered");
    db.update("UPDATE missions SET status='delivered' WHERE id=?", missionId);
    String body="<p>Le développeur <b>"+dev.get("name")+"</b> a livré le travail pour la mission <b>"+m.get("title")+"</b>.</p><p>Description: "+desc+"</p><p>Le lien que le développeur a mis: <a href='"+link+"'>"+link+"</a></p><p>Score qualité livraison: "+Math.round(quality)+"/100</p>";
    notify.email(String.valueOf(m.get("ownerEmail")),"Travail livré", notify.prettyEmail("Travail livré", body)); notify.notify(String.valueOf(m.get("ownerEmail")),"Travail livré avec lien: "+link,"delivery"); notify.workflow("Travail livré pour mission: "+m.get("title"));
  }
  public void validate(String missionId, Map<String,Object> b){
    String decision=val(b,"decision","validate");
    String reason=val(b,"reason","");
    String status="validate".equals(decision)?"validated":"revision_requested";
    db.update("UPDATE missions SET status=? WHERE id=?",status,missionId);
    Map<String,Object> m=mission(missionId);
    String clientEmail=String.valueOf(m.get("ownerEmail"));
    String devEmail=""; String devName="développeur";
    try { var dev=db.queryForMap("SELECT name,email FROM users WHERE id=?", String.valueOf(m.get("assignedDeveloperId"))); devEmail=String.valueOf(dev.get("email")); devName=String.valueOf(dev.get("name")); } catch(Exception ignored) {}
    String subject="validate".equals(decision)?"Travail validé":"Travail à corriger";
    String message="validate".equals(decision)
      ? "Le client a validé le travail livré pour la mission: "+m.get("title")
      : "Le client a demandé des corrections pour la mission: "+m.get("title")+(reason.isBlank()?"":". Motif: "+reason);
    notify.notify(clientEmail,message,"validation");
    if(!devEmail.isBlank()) notify.notify(devEmail,message,"validation");
    String html=notify.prettyEmail(subject,"<p>Bonjour <b>"+devName+"</b>,</p><p>"+message+"</p><p>Statut actuel: <b>"+status+"</b></p>");
    if(!devEmail.isBlank()) notify.email(devEmail,subject,html);
    notify.email(clientEmail,subject,notify.prettyEmail(subject,"<p>Confirmation côté client.</p><p>"+message+"</p>"));
    notify.workflow("Validation client: "+status+" sur mission "+missionId);
  }
  public void pay(String missionId){
    db.update("UPDATE missions SET status='paid' WHERE id=?",missionId);
    db.update("UPDATE users u JOIN missions m ON u.id=m.assigned_developer_id SET u.missions_completed=u.missions_completed+1,u.reputation=LEAST(100,u.reputation+3) WHERE m.id=?",missionId);
    Map<String,Object> m=mission(missionId);
    String clientEmail=String.valueOf(m.get("ownerEmail"));
    String devEmail=""; String devName="développeur";
    try { var dev=db.queryForMap("SELECT name,email FROM users WHERE id=?", String.valueOf(m.get("assignedDeveloperId"))); devEmail=String.valueOf(dev.get("email")); devName=String.valueOf(dev.get("name")); } catch(Exception ignored) {}
    String subject="Paiement effectué";
    String message="Le paiement simulé de la mission '"+m.get("title")+"' a été confirmé.";
    notify.notify(clientEmail,message,"payment");
    if(!devEmail.isBlank()) notify.notify(devEmail,message,"payment");
    String html=notify.prettyEmail(subject,"<p>Bonjour <b>"+devName+"</b>,</p><p>"+message+"</p><p>Votre réputation a été mise à jour automatiquement après paiement.</p>");
    if(!devEmail.isBlank()) notify.email(devEmail,subject,html);
    notify.email(clientEmail,subject,notify.prettyEmail(subject,"<p>Confirmation du paiement côté client.</p><p>"+message+"</p>"));
    notify.workflow("Paiement simulé mission "+missionId);
  }
  public Map<String,Object> analyze(String title, String description, List<String> skills, double budget){
    String cleaned=data.clean(title+" "+description); List<String> extracted=data.extractSkills(cleaned+" "+String.join(" ",skills)); if(extracted.isEmpty()) extracted=skills; String category=data.classify(cleaned,extracted); double complexity=data.complexity(description,extracted,budget); int days=data.estimateDays(complexity); double risk=data.risk(complexity,budget,days); double[] bud=data.estimateBudget(complexity,extracted);
    Map<String,Object> out=new LinkedHashMap<>(); out.put("cleanedDescription",cleaned);out.put("extractedSkills",extracted);out.put("category",category);out.put("complexityScore",Math.round(complexity));out.put("complexityLabel",data.complexityLabel(complexity));out.put("riskScore",Math.round(risk));out.put("estimatedBudgetMin",bud[0]);out.put("estimatedBudgetMax",bud[1]);out.put("estimatedDays",days);return out;
  }
  public List<Map<String,Object>> topTechnologies(){ Map<String,Long> counts=missions().stream().flatMap(m->((List<String>)m.get("skills")).stream()).collect(Collectors.groupingBy(s->s,Collectors.counting())); return counts.entrySet().stream().sorted((a,b)->Long.compare(b.getValue(),a.getValue())).map(e->Map.<String,Object>of("technology",e.getKey(),"count",e.getValue())).toList(); }
  public Map<String,Object> mapMission(Map<String,Object> r){ Map<String,Object> m=new LinkedHashMap<>(); m.put("id",r.get("id"));m.put("title",r.get("title"));m.put("description",r.get("description"));m.put("budget",r.get("budget"));m.put("skills",JsonUtil.splitSkills(r.get("skills")));m.put("status",r.get("status"));m.put("ownerEmail",r.get("owner_email"));m.put("assignedDeveloperId",r.get("assigned_developer_id"));m.put("category",r.get("category"));m.put("extractedSkills",JsonUtil.splitSkills(r.get("extracted_skills")));m.put("complexityScore",r.get("complexity_score"));m.put("complexityLabel",r.get("complexity_label"));m.put("riskScore",r.get("risk_score"));m.put("estimatedBudgetMin",r.get("estimated_budget_min"));m.put("estimatedBudgetMax",r.get("estimated_budget_max"));m.put("estimatedDays",r.get("estimated_days")); var del=db.queryForList("SELECT * FROM deliveries WHERE mission_id=? ORDER BY created_at DESC LIMIT 1", r.get("id")); if(!del.isEmpty()) {var d=del.get(0); m.put("work",Map.of("description",d.get("description"),"link",d.get("link"),"status",d.get("status"),"qualityScore",d.get("quality_score")));} return m; }
  private String findClientId(String email){ var r=db.queryForList("SELECT id FROM users WHERE LOWER(email)=? AND role='client'", email); if(r.isEmpty()){String id=JsonUtil.id("c");db.update("INSERT INTO users(id,name,email,password,role) VALUES(?,?,?,?,?)",id,"Client",email,"1234","client");return id;} return String.valueOf(r.get(0).get("id")); }
  private void sendBoth(String client, String dev, String subject, String msg){ notify.notify(client,msg,"decision");notify.notify(dev,msg,"decision");String html=notify.prettyEmail(subject,"<p>"+msg+"</p>");notify.email(client,subject,html);notify.email(dev,subject,html); }
  private String val(Map<String,Object>b,String k,String d){ Object v=b.get(k); return v==null||String.valueOf(v).isBlank()?d:String.valueOf(v); }
  private double num(Map<String,Object>b,String k,double d){ try{return Double.parseDouble(String.valueOf(b.getOrDefault(k,d)));}catch(Exception e){return d;} }
}
