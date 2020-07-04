package com.iptiq.providers;

import com.iptiq.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class AbstractProvider implements Provider, TaskExecutionListener {

    private Logger logger = LoggerFactory.getLogger(AbstractProvider.class);

    private ProviderThreadPoolExecutor executor;

    private ProviderStateListener providerStateListener;

    protected ProviderStatus status;

    // made protected just for the sake of the exercise which requires to use the ID in the get method
    protected final UUID uuid;

    public AbstractProvider(UUID uuid, ProviderStateListener providerStateListener) {
        this.uuid = uuid;
        this.providerStateListener = providerStateListener;
        status = ProviderStatus.OK;
        executor = new ProviderThreadPoolExecutor(ConfigProvider.getConfig().getLoadBalancer().getMaxConcurrentWorkersPerProvider(), this);
    }

    @Override
    public Future invokeProvider(Callable func) {
        Future invocation = executor.submit(func);
        if (this.status == ProviderStatus.OK && (executor.getTaskCount() - executor.getCompletedTaskCount()) >= executor.getMaximumPoolSize()) {
            logger.info("Reached max numbers of connections for " + uuid.toString() + ". switch to busy state");
            this.status = ProviderStatus.BUSY;
            providerStateListener.providerStateChanged(this, status);
        }
        return invocation;
    }

    @Override
    public void taskCompleted() {
        if (this.status == ProviderStatus.BUSY) {
            this.status = ProviderStatus.OK;
            providerStateListener.providerStateChanged(this, status);
        }
    }

    @Override
    public boolean tearDownService() {
        logger.info("tear down service : uid=" + uuid.toString());
        this.status = ProviderStatus.TEARDOWN;
        providerStateListener.providerStateChanged(this, status);

        executor.shutdown();

        // Gracefully wait for current executions fo proceed
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
            logger.info("service teared down properly: uid=" + this.uuid.toString());
        } catch (InterruptedException e) {
            logger.warn("tear down service error: " + e.toString());
            return false;
        }
        return true;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractProvider that = (AbstractProvider) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
