package com.relyon.parkhere.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
                    .uri("/reverse?lat={lat}&lon={lng}&format=json&addressdetails=1", lat, lng)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                log.debug("No response for ({}, {})", lat, lng);
                return null;
            }

            // Build clean address from structured data
            if (response.containsKey("address")) {
                var addr = (Map<String, String>) response.get("address");
                var clean = buildCleanAddress(addr);
                if (clean != null) {
                    log.debug("Reverse geocoded ({}, {}) -> {}", lat, lng, clean);
                    return clean;
                }
            }

            // Fallback to display_name if structured data unavailable
            if (response.containsKey("display_name")) {
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

    private String buildCleanAddress(Map<String, String> addr) {
        // Pick useful fields in order, skip verbose regional classifications
        var parts = new LinkedHashMap<String, String>();
        addIfPresent(parts, addr, "road");
        addIfPresent(parts, addr, "house_number");
        addIfPresent(parts, addr, "neighbourhood");
        addIfPresent(parts, addr, "suburb");
        addIfPresent(parts, addr, "city");
        addIfPresent(parts, addr, "town");
        addIfPresent(parts, addr, "state");
        addIfPresent(parts, addr, "postcode");
        addIfPresent(parts, addr, "country");

        if (parts.isEmpty()) return null;

        return parts.values().stream()
                .collect(Collectors.joining(", "));
    }

    private void addIfPresent(LinkedHashMap<String, String> parts, Map<String, String> addr, String key) {
        var value = addr.get(key);
        if (value != null && !value.isBlank()) {
            parts.put(key, value);
        }
    }
}
