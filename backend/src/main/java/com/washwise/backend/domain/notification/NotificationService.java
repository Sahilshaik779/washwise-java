package com.washwise.backend.domain.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    @Value("${app.mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${app.mailgun.domain}")
    private String mailgunDomain;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendEmail(String emailTo, String subject, String body) {
        if (mailgunApiKey == null || mailgunDomain == null || mailgunApiKey.isBlank()) {
            log.warn("Mailgun configuration missing. Skipping email to {}", emailTo);
            return;
        }

        String apiUrl = "https://api.mailgun.net/v3/" + mailgunDomain + "/messages";
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(("api:" + mailgunApiKey).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", authHeader);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("from", "WashWise Notifier <mailgun@" + mailgunDomain + ">");
        map.add("to", emailTo);
        map.add("subject", subject);
        map.add("html", body);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to send email. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", emailTo, e.getMessage());
        }
    }
}