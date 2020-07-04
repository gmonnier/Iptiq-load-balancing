package com.iptiq.registry;

import com.iptiq.providers.Action;
import com.iptiq.providers.Provider;
import com.iptiq.providers.ProviderActionFactory;
import com.iptiq.providers.ProviderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * ProviderHealthChecker implements a simple circuit breaker pattern for providers healths checks.
 * It unregisters any faulty provider instance from the load balancer registry.
 */
public class ProviderHealthChecker implements Runnable {

    private Logger logger = LoggerFactory.getLogger(ProviderHealthChecker.class);

    // Executor service holding the threads doing the checks
    private final ExecutorService checkThreadPool;

    // the Provider this health check should take care of.
    private final Provider provider;

    // the Registry this health check should take ba attached with
    private final ProviderRegistry registry;

    // Gap in milliseconds between two health check calls.
    private final int healthCheckFrequencyMillis;

    // Gap in milliseconds between two health check calls.
    private final int healthCheckRequestTimeoutMillis;

    // Circuit breaker state
    private CircuitBreakerState cbState;


    public ProviderHealthChecker(ExecutorService checkThreadPool, ProviderRegistry registry, Provider provider) {
        this.checkThreadPool = checkThreadPool;
        this.registry = registry;
        this.provider = provider;
        this.cbState = CircuitBreakerState.CLOSED;
        this.healthCheckFrequencyMillis = registry.getCurrentConfig().getHealthCheckFrequencyMillis();
        this.healthCheckRequestTimeoutMillis = registry.getCurrentConfig().getHealthCheckRequestTimeoutMillis();
    }

    public void start() {
        this.checkThreadPool.submit(this);
    }

    @Override
    public void run() {
        int consecutiveSuccessesChecks = 0;
        while (!Thread.interrupted()) {
            ProviderStatus status = ProviderStatus.OUT_OF_SERVICE;
            try {
                status = (ProviderStatus) provider.invokeProvider(new ProviderActionFactory(provider)
                        .getProviderAction(Action.HEALTH_CHECK))
                        .get(healthCheckRequestTimeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.warn("unable to reach check endpoint. timeout after " + healthCheckRequestTimeoutMillis + " ms");
            }

            if (status != ProviderStatus.OK && status != ProviderStatus.BUSY) {
                if (status == ProviderStatus.TEARDOWN) {
                    // stop health checks
                    Thread.currentThread().interrupt();
                    continue;
                }
                consecutiveSuccessesChecks = 0;
                if (cbState == CircuitBreakerState.CLOSED) {
                    logger.info("open circuit breaker for " + provider.getUuid().toString());
                    cbState = CircuitBreakerState.OPEN;
                    this.registry.deregisterProvider(provider);
                }
            } else {
                if (cbState == CircuitBreakerState.OPEN) {
                    consecutiveSuccessesChecks++;
                    if (consecutiveSuccessesChecks >= 2) {
                        cbState = CircuitBreakerState.CLOSED;
                        logger.info("close circuit breaker for " + provider.getUuid().toString());
                        try {
                            this.registry.registerProvider(provider);
                        } catch (RegistryOperationException roe) {
                            logger.warn("Unable to re-register instance after successful circuit breaker checks: ", roe.toString());
                            provider.tearDownService();
                            return;
                        }
                    }
                }
            }
            try {
                Thread.sleep(healthCheckFrequencyMillis);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
