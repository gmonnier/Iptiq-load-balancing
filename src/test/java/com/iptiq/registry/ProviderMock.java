package com.iptiq.registry;

import com.iptiq.providers.AbstractProvider;
import com.iptiq.providers.ProviderStateListener;
import com.iptiq.providers.ProviderStatus;

import java.util.UUID;

public class ProviderMock extends AbstractProvider {

    public ProviderMock(ProviderStateListener providerStateListener) {
        super(UUID.randomUUID(), providerStateListener);
    }

    @Override
    public String get() {
        return null;
    }

    @Override
    public ProviderStatus check() {
        return super.status;
    }

    public void setStatus(ProviderStatus status) {
        super.status = status;
    }

}
