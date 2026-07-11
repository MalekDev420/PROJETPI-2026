package com.gestionmissions.service;

import com.gestionmissions.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
  private final JdbcTemplate db; private final JavaMailSender mailSender;
  @Value("${spring.mail.username:}") private String from;
  public NotificationService(JdbcTemplate db, JavaMailSender mailSender){ this.db=db; this.mailSender=mailSender; }
  public void notify(String to, String message, String type){
    db.update("INSERT INTO notifications(id,recipient_email,message,type) VALUES(?,?,?,?)", JsonUtil.id("n"), to, message, type);
  }
  public void workflow(String message){ db.update("INSERT INTO workflow_events(id,message) VALUES(?,?)", JsonUtil.id("w"), message); }
  public void email(String to, String subject, String html){
    String status="logged_only";
    try {
      if(from != null && !from.isBlank()){
        var mime = mailSender.createMimeMessage(); var helper = new MimeMessageHelper(mime, true, "UTF-8");
        helper.setFrom(from); helper.setTo(to); helper.setSubject(subject); helper.setText(html, true); mailSender.send(mime); status="sent";
      }
    } catch(Exception e){ status="error: "+e.getClass().getSimpleName()+" - "+e.getMessage(); System.err.println("SMTP ERROR => "+status); }
    db.update("INSERT INTO email_logs(id,recipient_email,subject,body,status) VALUES(?,?,?,?,?)", JsonUtil.id("e"), to, subject, html.replaceAll("<[^>]*>", " ").replaceAll("\\s+"," ").trim(), status);
  }
  public String prettyEmail(String title, String body){
    return "<div style='font-family:Arial;background:#f5f7fb;padding:24px'><div style='max-width:650px;margin:auto;background:white;border-radius:18px;overflow:hidden;box-shadow:0 10px 30px #d0d4df'>"+
      "<div style='background:#111827;color:white;padding:24px'><h1 style='margin:0'>GM · Gestion Missions</h1><p style='margin:6px 0 0'>Plateforme freelance intelligente</p></div>"+
      "<div style='padding:24px'><h2>"+title+"</h2>"+body+"<hr><p style='color:#6b7280'>Email automatique généré par Gestion Missions.</p></div></div></div>";
  }
}
