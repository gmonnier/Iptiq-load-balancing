package com.iptiq.registry;

import com.iptiq.config.RegistryConfig;
import com.iptiq.providers.Provider;
import com.iptiq.providers.ProviderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InMemoryProviderRegistry implements ProviderRegistry {

    private Logger logger = LoggerFactory.getLogger(InMemoryProviderRegistry.class);

    // Executor service holding the threads doing the checks
    private final ExecutorService checkThreadPool = Executors.newCachedThreadPool();

    private final List<Provider> availableProviders;

    // reference to global provider registry configuration.
    private final RegistryConfig registryConfig;

    public InMemoryProviderRegistry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
        this.availableProviders = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public synchronized void registerProvider(Provider provider) throws RegistryOperationException {
        if (availableProviders.size() >= registryConfig.getMaxAllowedRegisteredProviders()) {
            throw new RegistryOperationException("Maximum providers limit reached");
        }

        if (availableProviders.contains(provider)) {
            throw new RegistryOperationException("Provider already registered");
        }

        availableProviders.add(provider);

        new ProviderHealthChecker(checkThreadPool, this, provider).start();
    }

    @Override
    public synchronized void deregisterProvider(Provider provider) {
        availableProviders.remove(provider);
    }

    @Override
    public List<Provider> getAvailableServices() {
        // expose immutable list to prevent any kind of external abuse
        return Collections.unmodifiableList(availableProviders);
    }

    @Override
    public RegistryConfig getCurrentConfig() {
        return registryConfig;
    }

    @Override
    public void providerStateChanged(Provider provider, ProviderStatus newStatus) {
        if (newStatus == ProviderStatus.BUSY || newStatus == ProviderStatus.TEARDOWN) {
            deregisterProvider(provider);
            return;
        }
        if (newStatus == ProviderStatus.OK) {
            availableProviders.add(provider);
            return;
        }
    }
}
