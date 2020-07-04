# Iptiq-load-balancing

**Notes on the libraries included**

As mentionned in the exercise description, no external framework has been included in this repo
 but the few libraries below required to perform minimal operations without having to reinvent the wheel:
    
    - `logback` for logging
    - `Jackson` for yaml config parsing
    - `JUnit` for unit testing
    
## Run and test application
Install dependencies and build application
`mvn install`

Run the application. It Creates a registry and a load balancer, and perform some operations then prints out some stats.
`mvn exec:java -Dexec.mainClass="com.iptiq.Application"`

Run JUnit tests.
`mvn test`

## IPTIQ Load Balancer design

<p align="center">
  <img src="https://user-images.githubusercontent.com/11952499/86527822-c65a2f80-bea2-11ea-86e4-093a10c10686.png">
</p>

### Step 1/2 – Registering providers

Registering providers is not performed directly on the load balancer itself, but rather on the registry attached to it, which is in charge of controlling and maintaining a list of available providers instances. The available state here directly means that the instance is ready for request processing.

Registering a provider is done as shown below:

```
            ProviderRegistry registry = new InMemoryProviderRegistry(...);
            LoadBalancer lb = new LoadBalancerBuilder().withRegistry(registry).build();
            
            Provider provider = new InMemoryProvider(registry);
            try {
                registry.registerProvider(provider);
            } catch (RegistryOperationException roe) {
                // Max number of providers registered reached.
                logger.error("Unable to register instance: " + roe.toString());
            }
```

### Step 3/4 – Load balancing strategies
Strategies logic defined in `selectNextAvailableProvider()` function into `src/main/java/com/iptiq/core/LoadBalancerImpl.java`

 - `RANDOM` set as default strategy in the configuration (`src/main/resources/application_config.yaml`)
 - `ROUND_ROBIN`

One thing to mention, is that regardless of the chosen strategy, the balancing operate on available providers only (ie providers with an `OK` state). Each provider has an internal state representation which evolves depending on its concurrent load allowed by the load balancer or intrinseque status. Here is below the state diagram of a provider:

<p align="center">
  <img src="https://user-images.githubusercontent.com/11952499/86527821-c5290280-bea2-11ea-9913-3e27d472ff25.png">
</p>

### Step 5 – Manual Inclusion / Exclusion
Those two operations are directly performed via the provider registry interface (`src/main/java/com/iptiq/registry/InMemoryProviderRegistry.java`):

```    
void registerProvider(Provider provider) throws RegistryOperationException;

void deregisterProvider(Provider provider);
```

### Step 6 and 7 – Health Check and Simplified Circuit breaker
Core logic of the health checker is define in the following file: `src/main/java/com/iptiq/registry/ProviderHealthChecker.java`

A simplified version of the circuit breaker pattern is implemented here (no half open state), and circuit breaker operate on the health request only.

As a side note, he health check is considered here as a request in itself. As such, it uses the same asynchronous invokation pattern than the get method. Those calls are then using the same connection (thread)  pool as the load balanced requests. Hence, be aware that frequents and heavy health checks might pressure the provider instance and make it flip to a BUSY state if capacity limit is reached.

### Step 8 – Capacity Limit
Here, a choice has been made to delegate the pressure control to the provider instance itself. instead of letting this responsibility the load balancer itself, each provider knows abouts its concurrency capacity. If full capacity comes to be reached, the provider will automatically set itself in a `BUSY` state, hence automatically deregistering from the providers registry.

If all the providers comes to be `BUSY`, no provider will be available and request won't be processed.

This approach mostly allows a more fined grained definition of the instance resources, which ultimately should be set dynamically at instance registration. In the case of this exercise, a single value representing the number of concurrent requests (Y) is put under configuration in `src/main/resources/application_config.yaml`
