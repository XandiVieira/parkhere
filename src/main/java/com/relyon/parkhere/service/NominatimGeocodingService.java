package com.relyon.parkhere.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "parkhere.geocoding.enabled", havingValue = "true")
public class NominatimGeocodingService implements GeocodingService {

    private final RestClient restClient;

    public NominatimGeocodingService() {
        this(RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "ParkHere/1.0")
                .build());
    }

    NominatimGeocodingService(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String reverseGeocode(double lat, double lng) {
        try {
            var response = restClient.get()
                    .uri("/reverse?lat={lat}&lon={lng}&format=json", lat, lng)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("display_name")) {
                var address = (String) response.get("display_name");
                log.debug("Reverse geocoded ({}, {}) -> {}", lat, lng, address);
                return address;
            }

            log.debug("No address found for ({}, {})", lat, lng);
            return null;
        } catch (Exception e) {
            log.warn("Reverse geocoding failed for ({}, {}): {}", lat, lng, e.getMessage());
            return null;
        }
    }
}
