package com.relyon.parkhere.model.enums;

public enum TrustLevel {
    HIGH,
    MEDIUM,
    LOW,
    NO_DATA;

    public static TrustLevel fromScore(double score) {
        if (score >= 0.7) return HIGH;
        if (score >= 0.4) return MEDIUM;
        if (score >= 0.1) return LOW;
        return NO_DATA;
    }
}
