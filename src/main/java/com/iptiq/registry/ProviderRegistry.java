package com.iptiq.registry;

import com.iptiq.config.RegistryConfig;
import com.iptiq.providers.Provider;
import com.iptiq.providers.ProviderStateListener;

import java.util.List;

public interface ProviderRegistry extends ProviderStateListener {

    void registerProvider(Provider provider) throws RegistryOperationException;

    void deregisterProvider(Provider provider);

    List<Provider> getAvailableServices();

    RegistryConfig getCurrentConfig();
}
