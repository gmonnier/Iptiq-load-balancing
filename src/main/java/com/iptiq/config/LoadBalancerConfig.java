package com.iptiq.config;

import com.iptiq.core.BalancingStrategy;

public class LoadBalancerConfig {

    private BalancingStrategy defaultStrategy;

    private int maxConcurrentWorkersPerProvider;

    public BalancingStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(BalancingStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public int getMaxConcurrentWorkersPerProvider() {
        return maxConcurrentWorkersPerProvider;
    }

    public void setMaxConcurrentWorkersPerProvider(int maxConcurrentWorkersPerProvider) {
        this.maxConcurrentWorkersPerProvider = maxConcurrentWorkersPerProvider;
    }
}
