package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class GeolocationService {

    private final RestTemplate rest = new RestTemplate();

    public String getCountryByIp(String ip) {
        try {
            String url = String.format("http://ip-api.com/json/%s?fields=status,country", ip);
            Map<?, ?> resp = rest.getForObject(url, Map.class);
            if ("success".equals(resp.get("status"))) {
                return (String) resp.get("country");
            } else {
                log.warn("Geolocation lookup failed for ip: " + ip);
            }
        } catch (Exception ignored) {
            log.error("Geolocation lookup error for ip: " + ip);
        }
        return "UNKNOWN";
    }
}