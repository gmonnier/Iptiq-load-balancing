package com.iptiq.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class InMemoryProvider extends AbstractProvider {

    private Logger logger = LoggerFactory.getLogger(InMemoryProvider.class);

    public InMemoryProvider(ProviderStateListener providerStateListener) {
        super(UUID.randomUUID(), providerStateListener);
        logger.info("new provider instance spawn up " + uuid.toString());
    }

    public String get() {
        logger.debug("invoked on " + uuid.toString());
        return uuid.toString();
    }

    public ProviderStatus check() {
        //logger.debug("health check on " + uuid.toString());
        return this.status;
    }

    @Override
    public String toString() {
        return "InMemoryProvider{" +
                "uuid=" + uuid +
                '}';
    }
}
