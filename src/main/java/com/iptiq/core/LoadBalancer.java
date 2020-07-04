package com.iptiq.core;

import com.iptiq.registry.ProviderRegistry;

import java.util.Map;
import java.util.concurrent.Future;

public interface LoadBalancer {

    // route exposed to the external world
    Future get() throws ServiceUnavailableException;

    // Return providers registry
    ProviderRegistry getRegistry();

    // Apply balancing strategy
    void setStrategy(BalancingStrategy strategy);

    // Return metrics to calls per provider ID
    Map<String, Integer> getMetrics();

    // Clear load balancer metrics
    void clearMetrics();
}
