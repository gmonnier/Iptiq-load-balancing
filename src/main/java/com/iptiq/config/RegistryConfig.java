package com.iptiq.config;

public class RegistryConfig {

    private int maxAllowedRegisteredProviders;

    private int healthCheckFrequencyMillis;

    private int healthCheckRequestTimeoutMillis;

    public int getMaxAllowedRegisteredProviders() {
        return maxAllowedRegisteredProviders;
    }

    public void setMaxAllowedRegisteredProviders(int maxAllowedRegisteredProviders) {
        this.maxAllowedRegisteredProviders = maxAllowedRegisteredProviders;
    }

    public int getHealthCheckFrequencyMillis() {
        return healthCheckFrequencyMillis;
    }

    public void setHealthCheckFrequencyMillis(int healthCheckFrequencyMillis) {
        this.healthCheckFrequencyMillis = healthCheckFrequencyMillis;
    }

    public int getHealthCheckRequestTimeoutMillis() {
        return healthCheckRequestTimeoutMillis;
    }

    public void setHealthCheckRequestTimeoutMillis(int healthCheckRequestTimeoutMillis) {
        this.healthCheckRequestTimeoutMillis = healthCheckRequestTimeoutMillis;
    }
}
