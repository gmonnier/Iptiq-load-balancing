package com.iptiq.registry;

import com.iptiq.config.ConfigProvider;
import com.iptiq.config.RegistryConfig;
import com.iptiq.providers.InMemoryProvider;
import com.iptiq.providers.Provider;
import com.iptiq.providers.ProviderStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryProviderRegistryTest {

    @Test
    void testProviderRegistryAddRemove() throws RegistryOperationException {

        InMemoryProviderRegistry registry = new InMemoryProviderRegistry(ConfigProvider.getConfig().getRegistry());

        Provider p1 = new ProviderMock(null);
        Provider p2 = new ProviderMock(null);

        registry.registerProvider(p1);
        registry.registerProvider(p2);

        assertEquals(2, registry.getAvailableServices().size());

        registry.deregisterProvider(p1);

        assertEquals(1, registry.getAvailableServices().size());
        assertEquals(p2, registry.getAvailableServices().get(0));

        registry.registerProvider(p1);
        assertEquals(2, registry.getAvailableServices().size());

        // Provider instance cannot be registered twice
        assertThrows(RegistryOperationException.class, () -> {
            registry.registerProvider(p2);
        });
        assertEquals(2, registry.getAvailableServices().size());
    }

    @Test
    void testProviderRegistryMaxLimitReached() {
        RegistryConfig config = ConfigProvider.getConfig().getRegistry();
        config.setMaxAllowedRegisteredProviders(10);

        InMemoryProviderRegistry registry = new InMemoryProviderRegistry(config);

        for (int i = 0; i < 10; i++) {
            Provider provider = new InMemoryProvider(registry);
            try {
                registry.registerProvider(provider);
            } catch (RegistryOperationException roe) {
                fail("Unexpected exception occured " + roe);
            }
        }

        assertEquals(10, registry.getAvailableServices().size());

        assertThrows(RegistryOperationException.class, () -> {
            registry.registerProvider(new InMemoryProvider(registry));
        });

    }

    @Test
    void testCircuitBreaker() throws InterruptedException {
        RegistryConfig config = new RegistryConfig();
        config.setMaxAllowedRegisteredProviders(2);
        int checkFrequency = 100;
        int asyncChecksTimeout = 5000;
        config.setHealthCheckFrequencyMillis(checkFrequency);
        config.setHealthCheckRequestTimeoutMillis(100);

        InMemoryProviderRegistry registry = new InMemoryProviderRegistry(config);

        ProviderMock provider1 = new ProviderMock(registry);
        ProviderMock provider2 = new ProviderMock(registry);

        try {
            registry.registerProvider(provider1);
            registry.registerProvider(provider2);
        } catch (RegistryOperationException roe) {
            fail("Unexpected exception occured " + roe);
        }

        List<Provider> providers = registry.getAvailableServices();
        assertEquals(2, providers.size());

        provider1.setStatus(ProviderStatus.OUT_OF_SERVICE);

        assertTimeout(Duration.ofMillis(asyncChecksTimeout), () -> {
            while (registry.getAvailableServices().size() > 1) {
                Thread.sleep(checkFrequency);
            }
        });
        assertEquals(provider2, providers.get(0));


        // Set the instance back to a working state
        provider1.setStatus(ProviderStatus.OK);

        assertTimeout(Duration.ofMillis(asyncChecksTimeout), () -> {
            while (registry.getAvailableServices().size() < 2) {
                Thread.sleep(checkFrequency);
            }
        });
        assertEquals(2, providers.size());

        provider1.tearDownService();
        assertEquals(1, providers.size());

    }
}
