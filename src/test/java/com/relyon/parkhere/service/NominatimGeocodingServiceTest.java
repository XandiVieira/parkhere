package com.relyon.parkhere.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NominatimGeocodingServiceTest {

    private RestClient restClient;
    private NominatimGeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        geocodingService = new NominatimGeocodingService(restClient);
    }

    @SuppressWarnings("unchecked")
    @Test
    void reverseGeocode_shouldReturnFormattedAddress() {
        var requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString(), any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("display_name", "Rua Voluntários da Pátria, Botafogo, Rio de Janeiro"));

        var address = geocodingService.reverseGeocode(-22.9527, -43.1862);

        assertEquals("Rua Voluntários da Pátria, Botafogo, Rio de Janeiro", address);
    }

    @SuppressWarnings("unchecked")
    @Test
    void reverseGeocode_shouldReturnNullOnApiError() {
        var requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString(), any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenThrow(new RuntimeException("API error"));

        var address = geocodingService.reverseGeocode(-22.9527, -43.1862);

        assertNull(address);
    }

    @SuppressWarnings("unchecked")
    @Test
    void reverseGeocode_shouldReturnNullOnEmptyResponse() {
        var requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString(), any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of());

        var address = geocodingService.reverseGeocode(-22.9527, -43.1862);

        assertNull(address);
    }
}
