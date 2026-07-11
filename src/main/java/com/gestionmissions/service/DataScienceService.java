package com.gestionmissions.service;

import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DataScienceService {
  private final Set<String> stopwords = Set.of("le","la","les","un","une","des","de","du","et","ou","avec","pour","dans","sur","je","veux","créer","creer","application","site","projet","mission","module","faire","the","a","an","to","with","and","or","of");
  private final Map<String,List<String>> categories = Map.of(
    "Frontend", List.of("angular","react","vue","html","css","typescript","dashboard","ui","ux"),
    "Backend", List.of("spring","java","node","express","api","rest","mysql","backend","security"),
    "Data", List.of("python","sql","machine","learning","prediction","data","cleaning","statistique","bi","powerbi"),
    "Mobile", List.of("flutter","android","ios","mobile","firebase"),
    "DevOps", List.of("docker","ci","cd","jenkins","devops","deploy","linux")
  );
  private final Map<String,Integer> techWeight = Map.ofEntries(
    Map.entry("angular",12),Map.entry("spring",14),Map.entry("mysql",8),Map.entry("api",8),Map.entry("dashboard",10),Map.entry("python",12),Map.entry("machine",12),Map.entry("learning",12),Map.entry("docker",10),Map.entry("flutter",10),Map.entry("security",10),Map.entry("prediction",12),Map.entry("cleaning",10)
  );

  public String clean(String text) {
    if(text == null) return "";
    String t = text.trim().replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ");
    t = t.replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", " ");
    t = t.replaceAll("(\\+?\\d[\\d .-]{7,}\\d)", " ");
    t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    return t.replaceAll("[^a-z0-9+#. ]", " ").replaceAll("\\s+", " ").trim();
  }
  public List<String> tokens(String text) {
    return Arrays.stream(clean(text).split(" ")).filter(s->s.length()>1 && !stopwords.contains(s)).distinct().toList();
  }
  public List<String> extractSkills(String text) {
    List<String> ts = tokens(text);
    List<String> skills = new ArrayList<>();
    Map<String,String> canonical = Map.ofEntries(
      Map.entry("angular","Angular"), Map.entry("spring","Spring Boot"), Map.entry("java","Java"), Map.entry("mysql","MySQL"), Map.entry("sql","SQL"), Map.entry("api","API"), Map.entry("dashboard","Dashboard"), Map.entry("python","Python"), Map.entry("machine","Machine Learning"), Map.entry("prediction","Prediction"), Map.entry("cleaning","Data Cleaning"), Map.entry("flutter","Flutter"), Map.entry("docker","Docker"), Map.entry("react","React"), Map.entry("node","Node.js")
    );
    for(String t: ts) if(canonical.containsKey(t)) skills.add(canonical.get(t));
    return skills.stream().distinct().toList();
  }
  public String classify(String text, List<String> skills) {
    String base = clean(text + " " + String.join(" ", skills));
    return categories.entrySet().stream().max(Comparator.comparingInt(e -> e.getValue().stream().mapToInt(k -> base.contains(k) ? 1 : 0).sum())).map(Map.Entry::getKey).orElse("General");
  }
  public double complexity(String description, List<String> skills, double budget) {
    List<String> ts = tokens(description + " " + String.join(" ", skills));
    int weighted = ts.stream().mapToInt(t -> techWeight.getOrDefault(t, 2)).sum();
    double len = Math.min(clean(description).length() / 10.0, 30);
    double skillPart = Math.min(skills.size() * 8.0, 40);
    double budgetPart = Math.min(budget / 100.0, 20);
    return clamp(weighted * 0.45 + len * 0.25 + skillPart * 0.2 + budgetPart * 0.1, 0, 100);
  }
  public String complexityLabel(double c){ if(c<30)return "Simple"; if(c<60)return "Moyenne"; if(c<82)return "Complexe"; return "Critique"; }
  public double risk(double complexity, double budget, int estimatedDays) {
    double lowBudgetRisk = complexity > 60 && budget < 600 ? 35 : complexity > 80 && budget < 1000 ? 20 : 5;
    double deadlineRisk = complexity > 75 && estimatedDays < 10 ? 30 : 8;
    return clamp(lowBudgetRisk + deadlineRisk + (complexity*0.25), 0, 100);
  }
  public int estimateDays(double complexity){ return Math.max(2, (int)Math.round(2 + complexity / 7.0)); }
  public double[] estimateBudget(double complexity, List<String> skills) {
    double base = 150 + complexity * 10 + skills.size()*80;
    return new double[]{Math.round(base * 0.85), Math.round(base * 1.25)};
  }
  public double cosine(List<String> a, List<String> b) {
    Set<String> vocab = new HashSet<>(); vocab.addAll(norm(a)); vocab.addAll(norm(b)); if(vocab.isEmpty()) return 0;
    Set<String> aa = new HashSet<>(norm(a)); Set<String> bb = new HashSet<>(norm(b));
    int dot=0; for(String v:vocab) if(aa.contains(v)&&bb.contains(v)) dot++;
    return dot / (Math.sqrt(aa.size()) * Math.sqrt(bb.size()));
  }
  public List<String> norm(List<String> s){ return s.stream().map(this::clean).filter(x->!x.isBlank()).collect(Collectors.toList()); }
  public double predictionSuccess(double matchScore, double reputation, String availability, int completed, int refusals, double risk) {
    double availabilityScore = "busy".equalsIgnoreCase(availability) ? 45 : 85;
    double history = Math.min(completed * 6.0, 30) - Math.min(refusals * 4.0, 20);
    return clamp(matchScore*0.45 + reputation*0.25 + availabilityScore*0.15 + history*0.1 - risk*0.15, 0, 100);
  }
  public String sentiment(String text) {
    String c = clean(text); int pos=0, neg=0;
    for(String w: List.of("excellent","bon","rapide","propre","satisfait","professionnel","merci")) if(c.contains(w)) pos++;
    for(String w: List.of("retard","mauvais","faible","bug","probleme","lent","refus")) if(c.contains(w)) neg++;
    if(pos>neg) return "positive"; if(neg>pos) return "negative"; return "neutral";
  }
  public double clamp(double v,double min,double max){ return Math.max(min, Math.min(max, v)); }
}
