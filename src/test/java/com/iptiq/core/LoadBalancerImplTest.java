package com.iptiq.core;

import com.iptiq.config.ConfigProvider;
import com.iptiq.config.LoadBalancerConfig;
import com.iptiq.config.RegistryConfig;
import com.iptiq.registry.ProviderRegistry;
import com.iptiq.registry.RegistryOperationException;
import com.iptiq.providers.InMemoryProvider;
import com.iptiq.providers.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

public class LoadBalancerImplTest {

    @BeforeEach
    void init() {
        RegistryConfig testRegConfig = new RegistryConfig();
        testRegConfig.setMaxAllowedRegisteredProviders(2);
        testRegConfig.setHealthCheckRequestTimeoutMillis(3000);
        testRegConfig.setHealthCheckFrequencyMillis(3000);

        LoadBalancerConfig loadBalancingConfig = new LoadBalancerConfig();
        loadBalancingConfig.setMaxConcurrentWorkersPerProvider(100);
        loadBalancingConfig.setDefaultStrategy(BalancingStrategy.RANDOM);

        ConfigProvider.getConfig().setRegistry(testRegConfig);
        ConfigProvider.getConfig().setLoadBalancer(loadBalancingConfig);
    }

    @Test
    void testLoadBalancerGetHappyPath() {
        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().build();

        ProviderRegistry registry = lb.getRegistry();
        Provider provider = new InMemoryProvider(registry);
        try {
            registry.registerProvider(provider);
        } catch (RegistryOperationException e) {
            fail("Unexpected exception occured " + e);
        }

        try {
            for (int i = 0; i < 100; i++) {
                handleSynchronousGet(lb);
            }
        } catch (ServiceUnavailableException e) {
            fail("Unexpected exception occured " + e);
        }

        assertEquals(100, lb.getMetrics().get(provider.getUuid().toString()).intValue());

    }

    @Test
    void testLoadBalancerUnavailableNoProviderRegistered() {
        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().build();

        ProviderRegistry registry = lb.getRegistry();

        // No provider registered
        assertThrows(ServiceUnavailableException.class, () -> {
            handleSynchronousGet(lb);
        });
    }

    @Test
    void testLoadBalancerUnavailableAllProvidersBusy() {
        ConfigProvider.getConfig().getLoadBalancer().setMaxConcurrentWorkersPerProvider(1);
        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().build();

        ProviderRegistry registry = lb.getRegistry();
        InMemoryProvider provider1 = new InMemoryProvider(registry);
        InMemoryProvider provider2 = new InMemoryProvider(registry);
        try {
            registry.registerProvider(provider1);
            registry.registerProvider(provider2);
        } catch (RegistryOperationException e) {
            fail("Unexpected exception occured " + e);
        }

        provider1.invokeProvider(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });
        provider2.invokeProvider(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });
        provider2.tearDownService();

        // One Provider teared down and one other is busy processing
        assertThrows(ServiceUnavailableException.class, () -> {
            handleSynchronousGet(lb);
        });
    }

    @Test
    void testLoadBalancerStrategies() throws ServiceUnavailableException {
        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().build();

        ProviderRegistry registry = lb.getRegistry();
        Provider provider1 = new InMemoryProvider(registry);
        Provider provider2 = new InMemoryProvider(registry);
        try {
            registry.registerProvider(provider1);
            registry.registerProvider(provider2);
        } catch (RegistryOperationException e) {
            fail("Unexpected exception occured " + e);
        }

        List<Provider> providers = lb.getRegistry().getAvailableServices();
        assertEquals(2, providers.size());

        lb.setStrategy(BalancingStrategy.ROUND_ROBIN);
        lb.clearMetrics();



        for (int i = 0; i < 100; i++) {
            handleSynchronousGet(lb);
            if (i < 2) {
                continue;
            }
            assertEquals(i / 2 + 1, lb.getMetrics().get(providers.get(0).getUuid().toString()).intValue(), "iteration " + i);
            assertEquals(i / 2 + i % 2, lb.getMetrics().get(providers.get(1).getUuid().toString()).intValue(), "iteration " + i);
        }


        // Change strategy on the fly - RANDOM
        lb.clearMetrics();
        lb.setStrategy(BalancingStrategy.RANDOM);

        int requestsCount = 1000;
        // maximum deviation allowed compared to a nominal evenly distributed value
        double maxDeviation = 0.5;
        for (int i = 0; i < requestsCount; i++) {
            handleSynchronousGet(lb);
        }
        double nominal = (double) requestsCount / lb.getRegistry().getAvailableServices().size();
        assertTrue(1 - Math.abs(nominal - (double) requestsCount / lb.getMetrics().get(provider1.getUuid().toString()).intValue() / nominal) < maxDeviation);
        assertTrue(1 - Math.abs(nominal - (double) requestsCount / lb.getMetrics().get(provider2.getUuid().toString()).intValue() / nominal) < maxDeviation);
    }

    public static void handleSynchronousGet(LoadBalancer lb) throws ServiceUnavailableException {
        Future f1 = lb.get();

        try {
            f1.get();
        } catch (InterruptedException | ExecutionException e) {
            fail("Unexpected exception occured " + e);
        }
    }

}
