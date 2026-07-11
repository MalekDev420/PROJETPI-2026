package com.gestionmissions.service;

import com.gestionmissions.util.JsonUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdvancedAlgorithmService {
  private final JdbcTemplate db;
  private final DataScienceService data;
  private final MissionService missionService;
  private final NotificationService notify;

  public AdvancedAlgorithmService(JdbcTemplate db, DataScienceService data, MissionService missionService, NotificationService notify) {
    this.db = db;
    this.data = data;
    this.missionService = missionService;
    this.notify = notify;
  }

  public Map<String,Object> analyzeMission(Map<String,Object> body) {
    String title = str(body.get("title"), "Mission sans titre");
    String description = str(body.get("description"), "");
    double budget = num(body.get("budget"), 500);
    List<String> skills = JsonUtil.splitSkills(body.get("skills"));
    Map<String,Object> basic = missionService.analyze(title, description, skills, budget);
    double complexity = num(basic.get("complexityScore"), 0);
    double risk = num(basic.get("riskScore"), 0);
    List<String> extracted = (List<String>) basic.getOrDefault("extractedSkills", List.of());
    String quality = descriptionQuality(description, budget, extracted);
    String requiredLevel = requiredLevel(complexity, risk);
    double clarity = clarityScore(description, extracted);
    double feasibility = clamp(100 - risk + (budget / Math.max(num(basic.get("estimatedBudgetMin"), 1), 1) * 10), 0, 100);
    Map<String,Object> out = new LinkedHashMap<>(basic);
    out.put("requiredLevel", requiredLevel);
    out.put("descriptionQuality", quality);
    out.put("clarityScore", Math.round(clarity));
    out.put("feasibilityScore", Math.round(feasibility));
    out.put("anomalies", missionAnomalies(title, description, budget, extracted, complexity, risk));
    out.put("recommendations", List.of(
      "Clarifier les livrables attendus",
      "Ajouter les technologies obligatoires",
      "Préciser délai, budget et critères d'acceptation",
      "Comparer au budget estimé avant publication"
    ));
    return out;
  }

  public List<Map<String,Object>> topDevelopersForMission(Map<String,Object> body) {
    Map<String,Object> analysis = analyzeMission(body);
    List<String> missionSkills = JsonUtil.splitSkills(analysis.get("extractedSkills"));
    if(missionSkills.isEmpty()) missionSkills = JsonUtil.splitSkills(body.get("skills"));
    String category = str(analysis.get("category"), "General");
    double risk = num(analysis.get("riskScore"), 0);
    double complexity = num(analysis.get("complexityScore"), 0);
    double budget = num(body.get("budget"), 500);
    List<Map<String,Object>> devs = db.queryForList("SELECT * FROM users WHERE role='developer'");
    final List<String> finalMissionSkills = missionSkills;
    return devs.stream().map(d -> scoreDeveloper(d, finalMissionSkills, category, risk, complexity, budget))
      .sorted((a,b) -> Long.compare(asLong(b.get("globalScore")), asLong(a.get("globalScore"))))
      .limit(5)
      .toList();
  }

  public Map<String,Object> predict(Map<String,Object> body) {
    String missionId = str(body.get("missionId"), "");
    String developerId = str(body.get("developerId"), "");
    Map<String,Object> mission = missionId.isBlank() ? analyzeMission(body) : missionService.mission(missionId);
    Map<String,Object> dev = db.queryForMap("SELECT * FROM users WHERE id=? AND role='developer'", developerId);
    List<String> mSkills = JsonUtil.splitSkills(mission.getOrDefault("skills", mission.get("extractedSkills")));
    if(mSkills.isEmpty()) mSkills = JsonUtil.splitSkills(mission.get("extractedSkills"));
    Map<String,Object> scored = scoreDeveloper(dev, mSkills, str(mission.get("category"), "General"), num(mission.get("riskScore"), 0), num(mission.get("complexityScore"), 0), num(mission.get("budget"), 500));
    double success = num(scored.get("successPrediction"), 0);
    Map<String,Object> out = new LinkedHashMap<>();
    Map<String,Object> devInfo = new LinkedHashMap<>();
    devInfo.put("id", dev.get("id"));
    devInfo.put("name", dev.get("name"));
    devInfo.put("email", dev.get("email"));
    out.put("developer", devInfo);
    out.put("missionTitle", mission.getOrDefault("title", str(body.get("title"), "Mission analysée")));
    out.put("successProbability", Math.round(success));
    out.put("riskLevel", success >= 80 ? "Faible" : success >= 60 ? "Moyen" : "Élevé");
    out.put("decision", success >= 70 ? "Recommandé" : "À vérifier manuellement");
    out.put("reasons", scored.get("explanation"));
    return out;
  }

  public List<Map<String,Object>> compareDevelopers(Map<String,Object> body) {
    List<String> ids = JsonUtil.splitSkills(body.get("developerIds"));
    if(ids.isEmpty()) ids = db.queryForList("SELECT id FROM users WHERE role='developer' LIMIT 3").stream().map(r -> String.valueOf(r.get("id"))).toList();
    List<String> missionSkills = JsonUtil.splitSkills(body.get("skills"));
    String category = str(body.get("category"), "General");
    double risk = num(body.get("riskScore"), 25);
    double complexity = num(body.get("complexityScore"), 50);
    double budget = num(body.get("budget"), 800);
    return ids.stream().map(id -> db.queryForMap("SELECT * FROM users WHERE id=?", id))
      .map(d -> scoreDeveloper(d, missionSkills, category, risk, complexity, budget))
      .sorted((a,b) -> Long.compare(asLong(b.get("globalScore")), asLong(a.get("globalScore"))))
      .toList();
  }

  public List<Map<String,Object>> scanAnomalies() {
    List<Map<String,Object>> out = new ArrayList<>();
    for(Map<String,Object> m : missionService.missions()) {
      double complexity = num(m.get("complexityScore"), 0);
      double budget = num(m.get("budget"), 0);
      double risk = num(m.get("riskScore"), 0);
      List<String> skills = JsonUtil.splitSkills(m.get("skills"));
      List<String> anomalies = missionAnomalies(str(m.get("title"),""), str(m.get("description"),""), budget, skills, complexity, risk);
      if(!anomalies.isEmpty()) out.add(Map.of("type", "Mission", "target", m.get("title"), "riskScore", Math.round(risk), "anomalies", anomalies));
    }
    for(Map<String,Object> d : db.queryForList("SELECT * FROM users WHERE role='developer'")) {
      List<String> anomalies = new ArrayList<>();
      if("busy".equalsIgnoreCase(str(d.get("availability"),""))) anomalies.add("Développeur marqué occupé");
      if(num(d.get("refusals"),0) >= 3) anomalies.add("Taux de refus élevé");
      if(num(d.get("rating"),0) < 3.5 && num(d.get("rating_count"),0) > 0) anomalies.add("Note moyenne faible");
      if(!anomalies.isEmpty()) out.add(Map.of("type", "Développeur", "target", d.get("name"), "riskScore", Math.round(100 - num(d.get("reputation"),50)), "anomalies", anomalies));
    }
    return out;
  }

  public Map<String,Object> optimizeBudget(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    double min = num(a.get("estimatedBudgetMin"), 0);
    double max = num(a.get("estimatedBudgetMax"), 0);
    double complexity = num(a.get("complexityScore"), 0);
    double risk = num(a.get("riskScore"), 0);
    double recommended = Math.round((min + max) / 2 + risk * 3);
    String strategy = complexity > 75 ? "Prévoir budget senior et marge de risque" : complexity > 45 ? "Budget moyen avec développeur confirmé" : "Budget simple acceptable";
    return Map.of("min", min, "max", max, "recommended", recommended, "strategy", strategy, "reason", "Calcul basé sur complexité, compétences, risque et niveau requis.");
  }

  public Map<String,Object> predictiveDashboard() {
    List<Map<String,Object>> missions = missionService.missions();
    double revenue = missions.stream().mapToDouble(m -> num(m.get("budget"),0)).sum();
    double predicted = missions.stream().mapToDouble(m -> num(m.get("budget"),0) * (1 - num(m.get("riskScore"),0)/180.0)).sum();
    List<Map<String,Object>> anomalies = scanAnomalies();
    List<Map<String,Object>> topDevs = db.queryForList("SELECT name,email,reputation,rating,missions_completed missionsCompleted,refusals FROM users WHERE role='developer' ORDER BY reputation DESC, rating DESC LIMIT 5");
    Map<String, Long> statusDist = missions.stream().collect(Collectors.groupingBy(m -> str(m.get("status"), "open"), Collectors.counting()));
    return Map.of(
      "potentialRevenue", Math.round(revenue),
      "predictedRevenue", Math.round(predicted),
      "riskCount", anomalies.size(),
      "statusDistribution", statusDist,
      "topDevelopers", topDevs,
      "alerts", anomalies.stream().limit(6).toList(),
      "decision", anomalies.size() > 3 ? "Surveiller les missions risquées" : "Situation globale stable"
    );
  }

  public Map<String,Object> explainMatching(Map<String,Object> body) {
    List<Map<String,Object>> top = topDevelopersForMission(body);
    return Map.of(
      "explanationType", "IA explicable / scoring multicritère",
      "formula", "Score = 35% compétences + 20% domaine + 15% réputation + 10% disponibilité + 10% historique + 10% budget/risque",
      "topResults", top,
      "note", "Le système ne donne pas seulement un résultat : il explique les facteurs de décision."
    );
  }

  public Map<String,Object> generateSpec(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    String title = str(body.get("title"), "Mission");
    List<String> skills = JsonUtil.splitSkills(a.get("extractedSkills"));
    String spec = "Cahier des charges généré\n\n" +
      "1. Objectif: réaliser " + title + ".\n" +
      "2. Catégorie détectée: " + a.get("category") + ".\n" +
      "3. Technologies: " + String.join(", ", skills) + ".\n" +
      "4. Complexité: " + a.get("complexityLabel") + " (" + a.get("complexityScore") + "/100).\n" +
      "5. Budget conseillé: " + a.get("estimatedBudgetMin") + " - " + a.get("estimatedBudgetMax") + " DT.\n" +
      "6. Délai estimé: " + a.get("estimatedDays") + " jours.\n" +
      "7. Critères d'acceptation: code source livré, lien Git, démonstration, validation client.\n" +
      "8. Risques: " + String.join("; ", missionAnomalies(title, str(body.get("description"),""), num(body.get("budget"),0), skills, num(a.get("complexityScore"),0), num(a.get("riskScore"),0))) + ".";
    return Map.of("specification", spec, "analysis", a);
  }

  public Map<String,Object> improveDescription(Map<String,Object> body) {
    String title = str(body.get("title"), "Mission").trim();
    String desc = cleanText(str(body.get("description"), ""));
    Map<String,Object> a = analyzeMission(body);

    List<String> skills = JsonUtil.splitSkills(a.get("extractedSkills"));
    if(skills.isEmpty()) skills = JsonUtil.splitSkills(body.get("skills"));

    String category = str(a.get("category"), inferCategoryFromText(title + " " + desc, skills));
    String objective = buildObjective(title, desc, category);
    List<String> deliverables = buildDeliverables(category, skills, desc);
    List<String> constraints = buildQualityConstraints(a, body);

    String technologies = skills.isEmpty()
      ? "à préciser selon le besoin technique"
      : String.join(", ", skills);

    String improved =
      "Mission : " + title + "\n\n" +
      "Objectif général : " + objective + "\n\n" +
      "Contexte et besoin : " + contextualizeDescription(desc, category) + "\n\n" +
      "Périmètre fonctionnel :\n- " + String.join("\n- ", deliverables) + "\n\n" +
      "Technologies et compétences détectées : " + technologies + ".\n\n" +
      "Contraintes et qualité attendue :\n- " + String.join("\n- ", constraints) + "\n\n" +
      "Estimation algorithmique : complexité " + a.get("complexityLabel") +
      ", délai estimé " + a.get("estimatedDays") + " jours, budget conseillé entre " +
      a.get("estimatedBudgetMin") + " et " + a.get("estimatedBudgetMax") + " DT.\n\n" +
      "Livrables finaux : code source versionné, lien Git ou Drive, documentation courte, démonstration et validation finale par le client.";

    return Map.of(
      "original", desc,
      "improved", improved,
      "analysis", a,
      "method", "Nettoyage texte + extraction compétences + génération dynamique selon catégorie, budget, complexité et technologies"
    );
  }


  public Map<String,Object> recommendBestDeveloper(Map<String,Object> body) {
    Map<String,Object> analysis = analyzeMission(body);
    List<Map<String,Object>> ranking = topDevelopersForMission(body);
    Map<String,Object> best = ranking.isEmpty() ? Map.of() : (Map<String,Object>) ranking.get(0).get("developer");
    String title = str(body.get("title"), "Mission");
    String clientEmail = str(body.get("ownerEmail"), "client@demo.com");
    String msg = best.isEmpty()
      ? "Aucun développeur optimal trouvé pour cette mission."
      : "Votre mission '" + title + "' est recommandée au développeur " + best.get("name") + " avec un score optimal basé sur les compétences, la réputation, la disponibilité et le budget.";
    if(!best.isEmpty()) {
      String devEmail = String.valueOf(best.get("email"));
      String missionId = str(body.get("id"), "");
      if(!missionId.isBlank()) {
        try { db.update("UPDATE missions SET assigned_developer_id=? WHERE id=? AND status='open'", best.get("id"), missionId); } catch(Exception ignored) {}
      }
      notify.notify(devEmail, "Mission recommandée par le client via IA: " + title + ". Consultez votre espace développeur pour accepter ou refuser.", "ai_recommendation");
      notify.notify(clientEmail, "Votre mission a été recommandée à " + best.get("name") + " par le laboratoire IA.", "ai_recommendation");
      notify.email(devEmail, "Mission recommandée par IA", notify.prettyEmail("Mission recommandée", "<p>Une mission ouverte vous est recommandée par l'algorithme IA.</p><p><b>Mission :</b> " + title + "</p><p>Consultez votre dashboard développeur pour accepter ou refuser.</p>"));
      notify.workflow("Mission ouverte recommandée à " + best.get("name") + " via Lab client: " + title);
    }
    Map<String,Object> out = new LinkedHashMap<>();
    out.put("analysis", analysis);
    out.put("ranking", ranking);
    out.put("bestDeveloper", best);
    out.put("clientEmail", clientEmail);
    out.put("recommendationMessage", msg);
    out.put("algorithm", "Ranking multicritère + similarité cosinus + prédiction de réussite");
    return out;
  }

  public Map<String,Object> publishOptimizedMission(Map<String,Object> body) {
    Map<String,Object> analysis = analyzeMission(body);
    Map<String,Object> budget = optimizeBudget(body);
    Map<String,Object> improved = improveDescription(body);
    Map<String,Object> enriched = new LinkedHashMap<>(body);
    enriched.put("description", improved.get("improved"));
    enriched.put("budget", budget.get("recommended"));
    enriched.put("skills", JsonUtil.csv(analysis.get("extractedSkills")));
    enriched.put("ownerEmail", str(body.get("ownerEmail"), "client@demo.com"));
    Map<String,Object> mission = missionService.create(enriched);

    List<Map<String,Object>> ranking = topDevelopersForMission(enriched);
    Map<String,Object> bestDeveloper = ranking.isEmpty() ? Map.of() : (Map<String,Object>) ranking.get(0).get("developer");
    String message = "Nouvelle mission optimisée recommandée pour vous: " + mission.get("title") + ". Consultez votre dashboard développeur pour accepter ou refuser.";
    if(!bestDeveloper.isEmpty()) {
      String devEmail = String.valueOf(bestDeveloper.get("email"));
      notify.notify(devEmail, message, "ai_recommendation");
      notify.email(devEmail, "Mission recommandée par IA", notify.prettyEmail("Mission recommandée", "<p>"+message+"</p><p>Score IA: "+ranking.get(0).get("globalScore")+"%</p>"));
      notify.workflow("Mission optimisée publiée et recommandée à "+bestDeveloper.get("name")+": "+mission.get("title"));
    }
    notify.notify(str(body.get("ownerEmail"), "client@demo.com"), "Mission publiée après optimisation IA: "+mission.get("title"), "mission_published");
    Map<String,Object> out = new LinkedHashMap<>();
    out.put("mission", mission);
    out.put("analysis", analysis);
    out.put("budgetOptimization", budget);
    out.put("improvedDescription", improved.get("improved"));
    out.put("ranking", ranking);
    out.put("bestDeveloper", bestDeveloper);
    out.put("message", "Mission publiée en état optimisé avec recommandation IA.");
    return out;
  }


  public Map<String,Object> deadlinePlan(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    int days = (int)Math.round(num(a.get("estimatedDays"), 10));
    double complexity = num(a.get("complexityScore"), 50);
    List<Map<String,Object>> phases = new ArrayList<>();
    int analyse = Math.max(1, (int)Math.round(days * 0.18));
    int design = Math.max(1, (int)Math.round(days * 0.16));
    int dev = Math.max(2, (int)Math.round(days * 0.42));
    int tests = Math.max(1, (int)Math.round(days * 0.16));
    int demo = Math.max(1, days - analyse - design - dev - tests);
    phases.add(Map.of("phase", "Analyse & cadrage", "days", analyse, "goal", "valider besoin, contraintes, données et livrables"));
    phases.add(Map.of("phase", "Conception", "days", design, "goal", "architecture, modèle de données et maquettes"));
    phases.add(Map.of("phase", "Développement", "days", dev, "goal", "implémentation fonctionnalités principales"));
    phases.add(Map.of("phase", "Tests & correction", "days", tests, "goal", "tests fonctionnels, bugs, qualité"));
    phases.add(Map.of("phase", "Livraison & démonstration", "days", demo, "goal", "Git/Drive, documentation courte, validation client"));
    String risk = complexity > 75 ? "Planning serré : prévoir marge de sécurité et développeur senior." : complexity > 50 ? "Planning équilibré avec points de contrôle intermédiaires." : "Planning simple et réalisable.";
    return Map.of("estimatedDays", days, "planningRisk", risk, "phases", phases, "method", "Répartition dynamique selon complexité NLP + score risque + type de mission");
  }

  public Map<String,Object> roiEstimate(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    double budget = num(body.get("budget"), 500);
    double risk = num(a.get("riskScore"), 30);
    double complexity = num(a.get("complexityScore"), 45);
    double productivityGain = clamp(12 + complexity * 0.45 - risk * 0.12, 5, 60);
    double paybackWeeks = Math.max(1, Math.round((budget / Math.max(150, productivityGain * 45)) * 10) / 10.0);
    double valueScore = clamp(productivityGain * 1.4 + (100-risk)*0.4 - Math.max(0, budget-1200)/80, 0, 100);
    String decision = valueScore >= 75 ? "Très rentable" : valueScore >= 55 ? "Rentabilité correcte" : "Rentabilité à justifier";
    return Map.of("productivityGainPercent", Math.round(productivityGain), "paybackWeeks", paybackWeeks, "businessValueScore", Math.round(valueScore), "decision", decision, "explanation", "Estimation basée sur budget, complexité, risque et valeur fonctionnelle détectée.");
  }

  public Map<String,Object> qualityChecklist(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    List<String> skills = JsonUtil.splitSkills(a.get("extractedSkills"));
    List<Map<String,Object>> checks = new ArrayList<>();
    checks.add(Map.of("item", "Description claire avec objectifs", "priority", "Haute", "status", num(a.get("clarityScore"),0) > 55 ? "OK" : "À améliorer"));
    checks.add(Map.of("item", "Critères d'acceptation définis", "priority", "Haute", "status", "À vérifier avant validation"));
    checks.add(Map.of("item", "Lien Git/Drive obligatoire à la livraison", "priority", "Haute", "status", "Obligatoire"));
    checks.add(Map.of("item", "Tests fonctionnels prévus", "priority", "Moyenne", "status", "Recommandé"));
    checks.add(Map.of("item", "Technologies détectées: " + (skills.isEmpty()?"à préciser":String.join(", ", skills)), "priority", "Moyenne", "status", skills.isEmpty()?"À compléter":"OK"));
    checks.add(Map.of("item", "Budget compatible avec complexité", "priority", "Haute", "status", num(a.get("riskScore"),0) > 65 ? "Risque" : "Acceptable"));
    double score = clamp(100 - num(a.get("riskScore"),0)*0.45 + num(a.get("clarityScore"),0)*0.35, 0, 100);
    return Map.of("qualityScore", Math.round(score), "checklist", checks, "decision", score >= 75 ? "Mission bien cadrée" : score >= 55 ? "Mission correcte mais améliorable" : "Mission à retravailler avant affectation");
  }

  public Map<String,Object> marketBenchmark(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    double current = num(body.get("budget"), 0);
    double min = num(a.get("estimatedBudgetMin"), 0);
    double max = num(a.get("estimatedBudgetMax"), 0);
    double median = Math.round((min+max)/2);
    String position = current < min ? "Sous le marché" : current > max ? "Au-dessus du marché" : "Dans la fourchette marché";
    double attractiveness = current <= 0 ? 40 : clamp((current / Math.max(median,1))*70 + (100-num(a.get("riskScore"),0))*0.25, 0, 100);
    return Map.of("currentBudget", current, "marketMin", min, "marketMedian", median, "marketMax", max, "marketPosition", position, "attractivenessScore", Math.round(attractiveness), "advice", current < min ? "Augmenter le budget pour attirer de bons développeurs." : "Budget exploitable pour lancer le matching.");
  }

  public Map<String,Object> missionHealth(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    Map<String,Object> budget = optimizeBudget(body);
    Map<String,Object> quality = qualityChecklist(body);
    Map<String,Object> roi = roiEstimate(body);
    double health = clamp(num(quality.get("qualityScore"),50)*0.35 + (100-num(a.get("riskScore"),0))*0.25 + num(roi.get("businessValueScore"),50)*0.25 + num(a.get("feasibilityScore"),50)*0.15, 0, 100);
    List<String> next = new ArrayList<>();
    if(num(a.get("riskScore"),0)>60) next.add("Réduire le risque : préciser délai, livrables et technologies.");
    if(num(a.get("clarityScore"),0)<60) next.add("Améliorer la description avant recommandation.");
    if(num(body.get("budget"),0) < num(budget.get("min"),0)) next.add("Budget inférieur à la recommandation : ajuster avant affectation.");
    if(next.isEmpty()) next.add("Mission prête pour recommandation au développeur optimal.");
    return Map.of("healthScore", Math.round(health), "status", health>=75?"Prête":health>=55?"Acceptable":"À optimiser", "nextActions", next, "analysis", a);
  }

  public Map<String,Object> milestonePlan(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    int days = (int)Math.round(num(a.get("estimatedDays"), 10));
    double budget = num(body.get("budget"), num(a.get("estimatedBudgetMax"), 900));
    List<Map<String,Object>> milestones = List.of(
      Map.of("name", "M1 - Cadrage validé", "percent", 15, "amount", Math.round(budget*0.15), "deliverable", "document besoin + périmètre"),
      Map.of("name", "M2 - Prototype / architecture", "percent", 25, "amount", Math.round(budget*0.25), "deliverable", "maquette ou architecture + schéma BD"),
      Map.of("name", "M3 - Fonctionnalités principales", "percent", 35, "amount", Math.round(budget*0.35), "deliverable", "module principal fonctionnel"),
      Map.of("name", "M4 - Tests et livraison", "percent", 25, "amount", Math.round(budget*0.25), "deliverable", "Git/Drive + démo + corrections")
    );
    return Map.of("estimatedDays", days, "totalBudget", Math.round(budget), "milestones", milestones, "benefit", "Découper la mission réduit le risque client et facilite le suivi du développeur.");
  }

  public Map<String,Object> techStackAdvice(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    String category = str(a.get("category"), "Fullstack");
    List<String> skills = JsonUtil.splitSkills(a.get("extractedSkills"));
    LinkedHashSet<String> recommended = new LinkedHashSet<>(skills);
    String lower = (str(body.get("title"),"") + " " + str(body.get("description"),"") + " " + String.join(" ", skills)).toLowerCase();
    if(category.contains("Data") || lower.contains("dashboard")) { recommended.add("Chart.js"); recommended.add("REST API"); recommended.add("Data Cleaning"); }
    if(lower.contains("spring") || lower.contains("backend")) { recommended.add("Spring Boot"); recommended.add("MySQL"); recommended.add("JPA"); }
    if(lower.contains("angular") || lower.contains("frontend")) { recommended.add("Angular"); recommended.add("TypeScript"); recommended.add("Responsive UI"); }
    if(recommended.isEmpty()) { recommended.add("Angular"); recommended.add("Spring Boot"); recommended.add("MySQL"); }
    return Map.of("category", category, "recommendedStack", new ArrayList<>(recommended), "architecture", "Frontend Angular → API Spring Boot → MySQL → SMTP/Ollama si nécessaire", "warning", num(a.get("riskScore"),0)>65?"Prévoir développeur confirmé/senior":"Stack cohérente pour cette mission");
  }

  public Map<String,Object> negotiationStrategy(Map<String,Object> body) {
    Map<String,Object> a = analyzeMission(body);
    Map<String,Object> b = optimizeBudget(body);
    double current = num(body.get("budget"),0);
    double recommended = num(b.get("recommended"),0);
    List<String> arguments = new ArrayList<>();
    arguments.add("Complexité calculée : " + a.get("complexityScore") + "/100 (" + a.get("complexityLabel") + ")");
    arguments.add("Risque calculé : " + a.get("riskScore") + "/100");
    arguments.add("Budget conseillé : " + b.get("min") + " - " + b.get("max") + " DT");
    arguments.add("Délai estimé : " + a.get("estimatedDays") + " jours");
    String strategy = current < recommended ? "Négocier une hausse du budget ou réduire le périmètre." : "Budget acceptable : négocier surtout la qualité et les jalons.";
    return Map.of("strategy", strategy, "recommendedBudget", recommended, "arguments", arguments, "clientBenefit", "Le client obtient des arguments rationnels avant de recommander la mission.");
  }

  private Map<String,Object> scoreDeveloper(Map<String,Object> d, List<String> missionSkills, String category, double risk, double complexity, double budget) {
    List<String> dSkills = JsonUtil.splitSkills(d.get("skills"));
    double skillScore = data.cosine(dSkills, missionSkills) * 100;
    double domainScore = str(d.get("domain"),"").equalsIgnoreCase(category) ? 100 : (String.join(" ", dSkills).toLowerCase().contains(category.toLowerCase()) ? 80 : 55);
    double reputation = num(d.get("reputation"), 50);
    double availability = "busy".equalsIgnoreCase(str(d.get("availability"),"")) ? 45 : 90;
    double history = clamp(num(d.get("missions_completed"),0) * 8 - num(d.get("refusals"),0) * 5 - num(d.get("delays"),0) * 6, 0, 100);
    double rate = num(d.get("rate"), 50);
    double budgetScore = clamp(100 - Math.abs((rate * Math.max(3, complexity/12)) - budget) / Math.max(budget, 1) * 60, 0, 100);
    double global = clamp(skillScore*.35 + domainScore*.20 + reputation*.15 + availability*.10 + history*.10 + budgetScore*.10 - risk*.06,0,100);
    double success = data.predictionSuccess(global, reputation, str(d.get("availability"),"available"), (int)num(d.get("missions_completed"),0), (int)num(d.get("refusals"),0), risk);
    List<String> explanation = List.of(
      "Compétences compatibles: " + Math.round(skillScore) + "%",
      "Domaine compatible: " + Math.round(domainScore) + "%",
      "Réputation: " + Math.round(reputation) + "/100",
      "Disponibilité: " + Math.round(availability) + "/100",
      "Historique missions: " + Math.round(history) + "/100",
      "Compatibilité budget: " + Math.round(budgetScore) + "%"
    );
    Map<String,Object> out = new LinkedHashMap<>();
    Map<String,Object> devInfo = new LinkedHashMap<>();
    devInfo.put("id", d.get("id"));
    devInfo.put("name", d.get("name"));
    devInfo.put("email", d.get("email"));
    devInfo.put("title", d.getOrDefault("title", "Développeur"));
    devInfo.put("domain", d.getOrDefault("domain", "General"));
    devInfo.put("image", d.getOrDefault("image", ""));
    devInfo.put("skills", dSkills);
    out.put("developer", devInfo);
    out.put("globalScore", Math.round(global)); out.put("skillScore", Math.round(skillScore)); out.put("domainScore", Math.round(domainScore)); out.put("reputationScore", Math.round(reputation)); out.put("availabilityScore", Math.round(availability)); out.put("historyScore", Math.round(history)); out.put("budgetScore", Math.round(budgetScore)); out.put("successPrediction", Math.round(success)); out.put("explanation", explanation); return out;
  }

  private String cleanText(String text) {
    if(text == null) return "";
    return text.trim().replaceAll("\\s+", " ").replaceAll("[<>]", "");
  }

  private String inferCategoryFromText(String text, List<String> skills) {
    String t = (text + " " + String.join(" ", skills)).toLowerCase();
    if(t.contains("mobile") || t.contains("android") || t.contains("ios") || t.contains("flutter")) return "Mobile";
    if(t.contains("data") || t.contains("dashboard") || t.contains("bi") || t.contains("machine learning") || t.contains("ia")) return "Data / IA";
    if(t.contains("devops") || t.contains("docker") || t.contains("ci/cd") || t.contains("kubernetes")) return "DevOps";
    if(t.contains("api") || t.contains("spring") || t.contains("backend") || t.contains("database")) return "Backend";
    if(t.contains("angular") || t.contains("react") || t.contains("frontend") || t.contains("interface")) return "Frontend";
    return "Fullstack";
  }

  private String buildObjective(String title, String desc, String category) {
    String base = desc.length() > 25 ? desc : title;
    String lower = (title + " " + desc).toLowerCase();
    if(lower.contains("dashboard") || lower.contains("statistique"))
      return "concevoir un tableau de bord décisionnel permettant de visualiser, filtrer et suivre les indicateurs clés liés à " + base + ".";
    if(lower.contains("login") || lower.contains("auth"))
      return "mettre en place un module sécurisé d'authentification et de gestion des rôles pour " + base + ".";
    if(lower.contains("site") || lower.contains("web"))
      return "développer une application web moderne, responsive et maintenable répondant au besoin suivant : " + base + ".";
    if(lower.contains("mobile"))
      return "développer une application mobile ergonomique répondant au besoin suivant : " + base + ".";
    return "réaliser une mission " + category + " structurée autour du besoin suivant : " + base + ".";
  }

  private String contextualizeDescription(String desc, String category) {
    if(desc == null || desc.length() < 20)
      return "Le besoin initial est encore bref. La mission doit être cadrée avec des objectifs, fonctionnalités, contraintes techniques et critères de validation.";
    return "À partir de la demande client, le projet est classé dans le domaine " + category + " et nécessite une réalisation structurée : analyse, conception, développement, tests et livraison.";
  }

  private List<String> buildDeliverables(String category, List<String> skills, String desc) {
    String text = (category + " " + String.join(" ", skills) + " " + desc).toLowerCase();
    LinkedHashSet<String> items = new LinkedHashSet<>();
    items.add("Analyse du besoin et clarification des fonctionnalités attendues");
    if(text.contains("dashboard") || text.contains("bi") || text.contains("data")) {
      items.add("Création de graphiques, indicateurs, filtres et cartes statistiques");
      items.add("Préparation ou nettoyage des données utilisées par les tableaux de bord");
    }
    if(text.contains("login") || text.contains("auth") || text.contains("role")) {
      items.add("Authentification, gestion des rôles et sécurisation des accès");
    }
    if(text.contains("api") || text.contains("spring") || text.contains("backend")) {
      items.add("Développement des API REST et logique métier côté serveur");
    }
    if(text.contains("mysql") || text.contains("database") || text.contains("base")) {
      items.add("Modélisation de la base de données et persistance des informations");
    }
    if(text.contains("angular") || text.contains("react") || text.contains("frontend") || text.contains("interface")) {
      items.add("Développement d'une interface utilisateur claire, responsive et moderne");
    }
    if(text.contains("mobile") || text.contains("android") || text.contains("ios")) {
      items.add("Développement des écrans mobiles principaux et navigation utilisateur");
    }
    items.add("Tests fonctionnels, correction des erreurs et préparation de la démonstration");
    return new ArrayList<>(items);
  }

  private List<String> buildQualityConstraints(Map<String,Object> analysis, Map<String,Object> body) {
    List<String> c = new ArrayList<>();
    c.add("Respecter une architecture propre et maintenable");
    c.add("Prévoir une validation client à la fin de la livraison");
    c.add("Documenter brièvement l'installation et l'utilisation");
    double risk = num(analysis.get("riskScore"), 0);
    double budget = num(body.get("budget"), 0);
    if(risk > 60) c.add("Surveiller les risques détectés : délai, budget ou complexité élevés");
    if(budget > 0) c.add("Respecter le budget annoncé ou proposer une justification en cas de dépassement");
    c.add("Livrer un lien Git/Drive fonctionnel contenant le travail final");
    return c;
  }

  private List<String> missionAnomalies(String title, String description, double budget, List<String> skills, double complexity, double risk) {
    List<String> a = new ArrayList<>();
    if(description == null || description.trim().length() < 30) a.add("Description trop courte ou floue");
    if(skills == null || skills.isEmpty()) a.add("Aucune compétence technique détectée");
    if(complexity > 70 && budget < 600) a.add("Budget faible par rapport à la complexité");
    if(risk > 65) a.add("Risque global élevé");
    if(title != null && title.toLowerCase().contains("urgent") && budget < 700) a.add("Mission urgente avec budget limité");
    return a;
  }
  private String descriptionQuality(String desc, double budget, List<String> skills) { double c = clarityScore(desc, skills); if(c>=80 && budget>0) return "Excellente"; if(c>=55) return "Correcte"; return "Faible"; }
  private double clarityScore(String desc, List<String> skills) { return clamp((desc==null?0:Math.min(desc.length()/2.0, 60)) + Math.min((skills==null?0:skills.size())*10,40), 0, 100); }
  private String requiredLevel(double c, double r) { if(c>80 || r>70) return "Senior"; if(c>55) return "Confirmé"; return "Junior/Confirmé"; }
  private String str(Object o, String d){ return o == null || String.valueOf(o).isBlank() ? d : String.valueOf(o); }
  private double num(Object o, double d){ try { return Double.parseDouble(String.valueOf(o)); } catch(Exception e){ return d; } }
  private long asLong(Object o){ return Math.round(num(o,0)); }
  private double clamp(double v, double min, double max){ return Math.max(min, Math.min(max, v)); }
}
