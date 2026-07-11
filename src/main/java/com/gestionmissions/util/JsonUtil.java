package com.gestionmissions.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class JsonUtil {
  private static final ObjectMapper mapper = new ObjectMapper();
  public static List<String> splitSkills(Object raw) {
    if (raw == null) return new ArrayList<>();
    if (raw instanceof List<?> list) return list.stream().map(String::valueOf).map(String::trim).filter(s->!s.isBlank()).toList();
    String s = String.valueOf(raw).trim();
    if (s.startsWith("[") && s.endsWith("]")) {
      try { return mapper.readValue(s, new TypeReference<List<String>>(){}); } catch(Exception ignored) {}
    }
    return Arrays.stream(s.split(",")).map(String::trim).filter(x->!x.isBlank()).toList();
  }
  public static String csv(Object raw) { return String.join(",", splitSkills(raw)); }
  public static String jsonList(Object raw) {
    try { return mapper.writeValueAsString(splitSkills(raw)); } catch(Exception e) { return "[]"; }
  }
  public static String id(String prefix) { return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10); }
  public static Map<String,Object> ok(String message) { return new LinkedHashMap<>(Map.of("message", message)); }
}
