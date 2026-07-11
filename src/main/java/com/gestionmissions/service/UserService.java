package com.gestionmissions.service;

import com.gestionmissions.util.JsonUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserService {
  private final JdbcTemplate db; private final NotificationService notify;
  public UserService(JdbcTemplate db, NotificationService notify){this.db=db;this.notify=notify;}
  public List<Map<String,Object>> developers(){ return mapDevs(db.queryForList("SELECT * FROM users WHERE role='developer' ORDER BY reputation DESC")); }
  public Map<String,Object> developer(String id){
    var rows = db.queryForList("SELECT * FROM users WHERE id=? AND role='developer'", id); if(rows.isEmpty()) throw new RuntimeException("Développeur introuvable");
    Map<String,Object> d = mapDev(rows.get(0)); d.put("reviews", db.queryForList("SELECT client_email clientEmail,rating,text,sentiment,created_at createdAt FROM ratings WHERE developer_id=? ORDER BY created_at DESC", id)); return d;
  }
  public Map<String,Object> createDeveloper(Map<String,Object> b){
    String id=JsonUtil.id("d");
    db.update("INSERT INTO users(id,name,email,password,role,title,domain,skills,level,rate,availability,image,bio,projects,rating,rating_count,reputation) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
      id, val(b,"name","Nouveau développeur"), val(b,"email", id+"@dev.local").toLowerCase(), val(b,"password","1234"), "developer", val(b,"title","Développeur"), val(b,"domain","Web"), JsonUtil.csv(b.get("skills")), val(b,"level","Confirmé"), num(b,"rate",60), val(b,"availability","available"), val(b,"image","https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400"), val(b,"bio",""), JsonUtil.jsonList(b.get("projects")), 0, 0, 50);
    return developer(id);
  }
  public Map<String,Object> updateDeveloper(String id, Map<String,Object> b){
    db.update("UPDATE users SET name=COALESCE(?,name),title=COALESCE(?,title),domain=COALESCE(?,domain),skills=COALESCE(?,skills),rate=COALESCE(?,rate),availability=COALESCE(?,availability),image=COALESCE(?,image),bio=COALESCE(?,bio) WHERE id=? AND role='developer'",
      b.get("name"), b.get("title"), b.get("domain"), b.containsKey("skills")?JsonUtil.csv(b.get("skills")):null, b.get("rate"), b.get("availability"), b.get("image"), b.get("bio"), id);
    return developer(id);
  }
  public void deleteDeveloper(String id){ db.update("DELETE FROM users WHERE id=? AND role='developer'", id); }
  public Map<String,Object> rate(String id, Map<String,Object> b){
    int rating = Math.max(1, Math.min(5, (int)num(b,"rating",5))); String text=val(b,"text",""); String sentiment = new DataScienceService().sentiment(text);
    db.update("INSERT INTO ratings(id,developer_id,client_email,rating,text,sentiment) VALUES(?,?,?,?,?,?)", JsonUtil.id("r"), id, val(b,"clientEmail","client@demo.com"), rating, text, sentiment);
    var a = db.queryForMap("SELECT AVG(rating) avgRating, COUNT(*) cnt FROM ratings WHERE developer_id=?", id);
    double avg = ((Number)a.get("avgRating")).doubleValue(); int cnt=((Number)a.get("cnt")).intValue(); int rep=(int)Math.min(100, Math.round(avg*16 + Math.min(cnt*2,20)));
    db.update("UPDATE users SET rating=?, rating_count=?, reputation=? WHERE id=?", avg, cnt, rep, id);
    return developer(id);
  }
  public Map<String,Object> auth(String role, Map<String,Object> b){
    String email=val(b,"email","").toLowerCase(); String pass=val(b,"password","1234");
    var rows=db.queryForList("SELECT * FROM users WHERE LOWER(email)=? AND role=?", email, role); if(rows.isEmpty()){
      if(role.equals("client")) return registerClient(Map.of("email",email,"password",pass,"name",val(b,"name","Client")));
      throw new RuntimeException("Compte introuvable");
    }
    var u=rows.get(0); if(!Objects.equals(String.valueOf(u.get("password")), pass)) throw new RuntimeException("Mot de passe incorrect");
    return role.equals("developer") ? developer(String.valueOf(u.get("id"))) : mapUser(u);
  }
  public Map<String,Object> registerClient(Map<String,Object> b){
    String id=JsonUtil.id("c"), email=val(b,"email",id+"@client.local").toLowerCase();
    db.update("INSERT IGNORE INTO users(id,name,email,password,role,company,reputation) VALUES(?,?,?,?,?,?,?)", id, val(b,"name","Client"), email, val(b,"password","1234"), "client", val(b,"company",""), 50);
    return db.queryForList("SELECT * FROM users WHERE email=?", email).stream().findFirst().map(this::mapUser).orElse(Map.of("email",email));
  }
  public String forgot(String role, String email){
    String temp="GM" + new Random().nextInt(900000)+100000;
    db.update("UPDATE users SET password=? WHERE LOWER(email)=? AND role=?", temp, email.toLowerCase(), role);
    notify.email(email, "Nouveau mot de passe", notify.prettyEmail("Récupération mot de passe", "<p>Votre nouveau mot de passe temporaire est :</p><h2>"+temp+"</h2>")); return temp;
  }
  public List<Map<String,Object>> mapDevs(List<Map<String,Object>> rows){ return rows.stream().map(this::mapDev).toList(); }
  public Map<String,Object> mapDev(Map<String,Object> r){ Map<String,Object> m=mapUser(r); m.put("title",r.get("title"));m.put("domain",r.get("domain"));m.put("skills",JsonUtil.splitSkills(r.get("skills")));m.put("level",r.get("level"));m.put("rate",r.get("rate"));m.put("availability",r.get("availability"));m.put("image",r.get("image"));m.put("bio",r.get("bio"));m.put("projects",JsonUtil.splitSkills(r.get("projects")));m.put("rating",r.get("rating"));m.put("ratingCount",r.get("rating_count"));m.put("reputation",r.get("reputation")); return m; }
  public Map<String,Object> mapUser(Map<String,Object> r){ Map<String,Object> m=new LinkedHashMap<>(); for(String k:List.of("id","name","email","role","company","password")) m.put(k,r.get(k)); return m; }
  private String val(Map<String,Object>b,String k,String d){ Object v=b.get(k); return v==null||String.valueOf(v).isBlank()?d:String.valueOf(v); }
  private double num(Map<String,Object>b,String k,double d){ try{return Double.parseDouble(String.valueOf(b.getOrDefault(k,d)));}catch(Exception e){return d;} }
}
