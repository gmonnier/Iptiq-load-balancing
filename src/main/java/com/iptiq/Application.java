package com.iptiq;

import com.iptiq.config.ConfigProvider;
import com.iptiq.core.BalancingStrategy;
import com.iptiq.core.LoadBalancer;
import com.iptiq.core.LoadBalancerBuilder;
import com.iptiq.core.ServiceUnavailableException;
import com.iptiq.providers.InMemoryProvider;
import com.iptiq.providers.Provider;
import com.iptiq.registry.ProviderRegistry;
import com.iptiq.registry.RegistryOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Application {
    static Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Start load balancer application");
        LoadBalancer lb = new LoadBalancerBuilder().build();
        ProviderRegistry registry = lb.getRegistry();

        // --------> STEP #2 - Register 10 balancers
        for (int i = 0; i < 10; i++) {
            Provider provider = new InMemoryProvider(registry);
            try {
                registry.registerProvider(provider);
            } catch (RegistryOperationException roe) {
                logger.error("Unable to register instance: " + roe.toString());
            }
        }


        // --------> STEP #3 - Load Balancer operates on a Random mode by default. Try out to execute few requests
        callAndPrintStats(lb);


        // --------> STEP #4 - Change the strategy of the load balancer to ROUND_ROBIN
        lb.clearMetrics();
        lb.setStrategy(BalancingStrategy.ROUND_ROBIN);
        callAndPrintStats(lb);


        // --------> STEP #5 - Include / Exclude provider to the registry
        List<Provider> availableProviders = registry.getAvailableServices();
        Provider p1 = availableProviders.get(0);
        Provider p2 = availableProviders.get(1);

        // Exclude two providers
        registry.deregisterProvider(p1);
        registry.deregisterProvider(p2);

        p1.tearDownService();
        p2.tearDownService();
        callAndPrintStats(lb);

        // Manually add a new provider
        Provider newProvider = new InMemoryProvider(registry);
        try {
            registry.registerProvider(newProvider);
        } catch (RegistryOperationException e) {
            e.printStackTrace();
        }
        callAndPrintStats(lb);

        // Tear down services
        for (int i = 0; i < registry.getAvailableServices().size(); i++) {
            registry.getAvailableServices().get(i).tearDownService();
        }

        // --------> STEP #7  - Health checks and circuit breaker
        // Checked at src/test/java/com/iptiq/registry/InMemoryProviderRegistryTest.java

        // --------> STEP #8  - Max capacity
        lb = new LoadBalancerBuilder().build();
        ConfigProvider.getConfig().getLoadBalancer().setMaxConcurrentWorkersPerProvider(2);
        registry = lb.getRegistry();

        p1 = new InMemoryProvider(registry);
        p2 = new InMemoryProvider(registry);
        logger.info("STEP 8: check max capacity");
        try {
            registry.registerProvider(p1);
            registry.registerProvider(p2);
        } catch (RegistryOperationException e) {
            e.printStackTrace();
        }

        // Force all providers to be busy with long tasks running
        p1.invokeProvider(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });
        p1.invokeProvider(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });
        p2.invokeProvider(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });
        p2.invokeProvider(() -> {
            while (true) {
                Thread.sleep(1000);
            }
        });


        try {
            logger.info("STEP 8: Check sync get");
            handleSynchronousGet(lb);
            logger.info("STEP 8: Check sync done");
        } catch (ServiceUnavailableException e) {
            logger.info("STEP 8: Expected service unavailable exception raised");
        }

        // Tear down services
        for (int i = 0; i < registry.getAvailableServices().size(); i++) {
            registry.getAvailableServices().get(i).tearDownService();
        }

        System.exit(0);
    }

    private static void callAndPrintStats(LoadBalancer lb) {
        try {
            for (int i = 0; i < 100; i++) {
                handleSynchronousGet(lb);
            }
        } catch (Exception e) {
            logger.warn("Unable to process request: " + e.toString());
        }

        logger.info("Load Balancer Metrics - request distribution -->");
        printMetrics(lb);
        logger.info("<-- Load Balancer Metrics Strategy");
    }

    private static void printMetrics(LoadBalancer lb1) {
        Map<String, Integer> metrics = lb1.getMetrics();
        int totalCalls = 0;
        for (Map.Entry<String, Integer> entry : metrics.entrySet()) {
            totalCalls += entry.getValue();
            logger.info("# of requests for: " + entry.getKey() + " -- " + entry.getValue());
        }
        logger.info("total calls: " + totalCalls);
    }

    /*
     * Perform synchronous call on the lb. Ideally a web server would hold a connection pool at that level to manage
     * concurrency on responses. I guess this is out of scope for this exercise.
     */
    public static void handleSynchronousGet(LoadBalancer lb) throws ServiceUnavailableException {
        Future f1 = lb.get();

        try {
            f1.get();
        } catch (InterruptedException e) {
            logger.error("Connection interrupted by peer" + e.toString());
        } catch (ExecutionException e) {
            logger.error("Connection interrupted by peer" + e.toString());
        }
    }
}
