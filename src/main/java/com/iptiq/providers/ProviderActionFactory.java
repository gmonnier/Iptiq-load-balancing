package com.iptiq.providers;

import java.util.concurrent.Callable;

public class ProviderActionFactory {

    private final Provider provider;

    public ProviderActionFactory(Provider provider) {
        this.provider = provider;
    }

    public Callable getProviderAction(Action action) {
        switch (action) {
            case LONG_GET:
                return actionLongGet();
            case HEALTH_CHECK:
                return actionCheck();
            default:
                return actionGet();
        }
    }

    private Callable<ProviderStatus> actionCheck() {
        return () -> provider.check();
    }

    private Callable<String> actionGet() {
        return () -> provider.get();
    }

    private Callable<String> actionLongGet() {
        return () -> {
            Thread.sleep(5000);
            return provider.get();
        };
    }
}
