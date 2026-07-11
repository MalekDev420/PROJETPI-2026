package com.gestionmissions.controller;

import com.gestionmissions.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {
    private final NotificationService notificationService;

    @Value("${spring.mail.username:}")
    private String smtpEmail;

    public EmailTestController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean configured = smtpEmail != null && !smtpEmail.isBlank();
        return Map.of(
                "configured", configured,
                "from", configured ? smtpEmail : "SMTP non configuré",
                "message", configured
                        ? "SMTP Gmail configuré. Vous pouvez tester l'envoi."
                        : "Ajoutez SMTP_EMAIL et SMTP_PASSWORD ou configurez application.properties."
        );
    }

    @PostMapping("/test")
    public Map<String, Object> testEmail(@RequestBody(required = false) Map<String, String> body) {
        String to = body != null ? body.getOrDefault("to", smtpEmail) : smtpEmail;
        if (to == null || to.isBlank()) {
            return Map.of(
                    "success", false,
                    "message", "Aucun email destinataire. Envoyez {\"to\":\"votre.email@gmail.com\"}."
            );
        }

        String html = notificationService.prettyEmail(
                "Test SMTP Spring Boot",
                "<p>Ce message confirme que Gmail SMTP fonctionne dans le backend Spring Boot.</p>" +
                "<p><b>Plateforme :</b> Gestion Missions</p>" +
                "<p><b>Module :</b> Notifications automatiques client / développeur</p>"
        );

        notificationService.email(to, "Test SMTP - Gestion Missions Spring", html);

        return Map.of(
                "success", true,
                "message", "Demande d'envoi traitée. Vérifiez votre boîte email et la table email_logs.",
                "to", to
        );
    }
}
