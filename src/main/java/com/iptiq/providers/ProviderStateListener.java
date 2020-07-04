package com.iptiq.providers;

public interface ProviderStateListener {
    void providerStateChanged(Provider provider, ProviderStatus newStatus);
}
