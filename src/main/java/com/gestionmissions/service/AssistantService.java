package com.gestionmissions.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.Duration;
import java.util.*;

@Service
public class AssistantService {
  @Value("${ollama.base-url}") private String baseUrl;
  @Value("${ollama.model}") private String model;
  private final MissionService missions;
  public AssistantService(MissionService missions){this.missions=missions;}
  public Map<String,Object> status(){
    try { RestClient.create(baseUrl).get().uri("/api/tags").retrieve().body(String.class); return Map.of("available",true,"model",model,"baseUrl",baseUrl); }
    catch(Exception e){ return Map.of("available",false,"model",model,"error",e.getMessage()); }
  }
  public String chat(String message){
    String context = "Tu es l'assistant IA de la plateforme Gestion Missions. Réponds en français, de façon claire et utile. La plateforme contient: Angular, Spring Boot, MySQL, matching IA, scoring, data cleaning, NLP, prédiction, réputation développeur, livraison GitHub, SMTP. Question: "+message;
    try {
      Map<String,Object> body = new LinkedHashMap<>(); body.put("model", model); body.put("stream", false); body.put("messages", List.of(Map.of("role","user","content",context)));
      Map resp = RestClient.create(baseUrl).post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
      Object msg = ((Map)resp.get("message")).get("content"); if(msg != null && !String.valueOf(msg).isBlank()) return String.valueOf(msg);
    } catch(Exception ignored) {}
    return fallback(message);
  }
  private String fallback(String m){ String s=m.toLowerCase(); if(s.contains("matching")) return "Le matching calcule un score multicritère: similarité cosinus des compétences, domaine, réputation, disponibilité, risque et prédiction de réussite."; if(s.contains("mission")) return "Pour créer une mission, le client renseigne titre, description, budget et compétences. Le backend nettoie le texte, extrait les mots-clés et calcule complexité, risque et budget estimé."; if(s.contains("spring")) return "Le backend Spring Boot expose des API REST et sépare routes/controllers/services pour garder une architecture maintenable."; return "Je peux vous aider sur missions, développeurs, matching IA, nettoyage de données, prédiction, livraison et dashboards."; }
}
