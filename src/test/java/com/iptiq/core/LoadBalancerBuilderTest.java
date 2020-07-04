package com.iptiq.core;

import com.iptiq.config.ConfigProvider;
import com.iptiq.config.RegistryConfig;
import com.iptiq.providers.Provider;
import com.iptiq.providers.ProviderStatus;
import com.iptiq.registry.ProviderRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadBalancerBuilderTest {
    @Test
    void testDefaultLoadBalancerBuild() {
        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().build();

        assertEquals(ConfigProvider.getConfig().getLoadBalancer().getDefaultStrategy(), lb.getStrategy());
    }

    @Test
    void testCustomStrategyLoadBalancerBuild() {
        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().withBalancingStrategy(BalancingStrategy.ROUND_ROBIN).build();

        assertEquals(BalancingStrategy.ROUND_ROBIN, lb.getStrategy());
    }

    @Test
    void testCustomRegistryLoadBalancerBuild() {

        ProviderRegistry testRegistry = new ProviderRegistry() {
            @Override
            public void registerProvider(Provider provider) {
            }

            @Override
            public void deregisterProvider(Provider provider) {
            }

            @Override
            public List<Provider> getAvailableServices() {
                return null;
            }

            @Override
            public RegistryConfig getCurrentConfig() {
                return null;
            }

            @Override
            public void providerStateChanged(Provider provider, ProviderStatus newStatus) {
            }
        };

        LoadBalancerImpl lb = (LoadBalancerImpl) new LoadBalancerBuilder().withRegistry(testRegistry).build();

        assertTrue(lb.getRegistry() == testRegistry);
    }
}
