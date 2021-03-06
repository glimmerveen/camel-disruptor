/*
 * Copyright 2012 Riccardo Sirchia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.camel.component.disruptor;

import com.lmax.disruptor.dsl.ProducerType;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.api.management.ManagedOperation;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TODO: documentation
 */
public class DisruptorEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisruptorEndpoint.class);
    public static final String DISRUPTOR_IGNORE_EXCHANGE = "disruptor.ignoreExchange";

    private final int concurrentConsumers;
    private final boolean multipleConsumers;
    private WaitForTaskToComplete waitForTaskToComplete = WaitForTaskToComplete.IfReplyExpected;

    private long timeout = 30000;

    private final Set<DisruptorProducer> producers = new CopyOnWriteArraySet<DisruptorProducer>();
    private final Set<DisruptorConsumer> consumers = new CopyOnWriteArraySet<DisruptorConsumer>();

    private final DisruptorReference disruptorReference;

    public DisruptorEndpoint(final String endpointUri, final Component component, final DisruptorReference disruptorReference, final int concurrentConsumers, final boolean multipleConsumers) throws Exception {
        super(endpointUri, component);
        this.disruptorReference = disruptorReference;
        this.concurrentConsumers = concurrentConsumers;
        this.multipleConsumers = multipleConsumers;
    }

    @ManagedAttribute(description = "Buffer max capacity")
    public int getSize() {
        return disruptorReference.getBufferSize();
    }

    @ManagedOperation(description = "Remaining capacity in ring buffer")
    public long remainingCapacity() throws DisruptorNotStartedException {
        return getDisruptor().remainingCapacity();
    }


    @ManagedAttribute(description = "Number of concurrent consumers")
    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public WaitForTaskToComplete getWaitForTaskToComplete() {
        return waitForTaskToComplete;
    }

    public void setWaitForTaskToComplete(final WaitForTaskToComplete waitForTaskToComplete) {
        this.waitForTaskToComplete = waitForTaskToComplete;
    }

    @ManagedAttribute
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }

    @ManagedAttribute
    public boolean isMultipleConsumers() {
        return multipleConsumers;
    }

    /**
     * Returns the current active consumers on this endpoint
     */
    public Set<DisruptorConsumer> getConsumers() {
        return Collections.unmodifiableSet(consumers);
    }

    /**
     * Returns the current active producers on this endpoint
     */
    public Set<DisruptorProducer> getProducers() {
        return Collections.unmodifiableSet(producers);
    }

    @Override
    @ManagedAttribute
    public boolean isMultipleConsumersSupported() {
        return isMultipleConsumers();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Producer createProducer() throws Exception {
        if (getProducers().size() == 1 && getDisruptor().getProducerType() == ProducerType.SINGLE) {
            throw new IllegalStateException("Endpoint can't support multiple producers when ProducerType SINGLE is configured");
        }
        return new DisruptorProducer(this, getWaitForTaskToComplete(), getTimeout());
    }

    @Override
    public Consumer createConsumer(final Processor processor) throws Exception {
        return new DisruptorConsumer(this, processor);
    }

    @Override
    protected void doStart() throws Exception {
        // notify reference we are shutting down this endpoint
        disruptorReference.addEndpoint(this);

        super.doStart();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    protected void doStop() throws Exception {
        // notify reference we are shutting down this endpoint
        disruptorReference.removeEndpoint(this);

        super.doStop();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public DisruptorComponent getComponent() {
        return (DisruptorComponent) super.getComponent();
    }

    void onStarted(final DisruptorConsumer consumer) throws Exception {
        synchronized (this) {
            if (consumers.add(consumer)) {
                LOGGER.debug("Starting consumer {} on endpoint {}", consumer, getEndpointUri());

                getDisruptor().reconfigure();

            } else {
                LOGGER.debug("Tried to start Consumer {} on endpoint {} but it was already started", consumer,
                        getEndpointUri());
            }
        }

    }


    void onStopped(final DisruptorConsumer consumer) throws Exception {
        synchronized (this) {

            if (consumers.remove(consumer)) {
                LOGGER.debug("Stopping consumer {} on endpoint {}", consumer, getEndpointUri());

                getDisruptor().reconfigure();

            } else {
                LOGGER.debug("Tried to stop Consumer {} on endpoint {} but it was already stopped", consumer,
                        getEndpointUri());
            }


        }
    }

    void onStarted(final DisruptorProducer producer) {
        producers.add(producer);
    }

    void onStopped(final DisruptorProducer producer) {
        producers.remove(producer);
    }

    Collection<LifecycleAwareExchangeEventHandler> createConsumerEventHandlers() {
        final List<LifecycleAwareExchangeEventHandler> eventHandlers = new ArrayList<LifecycleAwareExchangeEventHandler>();

        for (final DisruptorConsumer consumer : consumers) {
            eventHandlers.addAll(consumer.createEventHandlers(concurrentConsumers));
        }

        return eventHandlers;
    }

    /**
     * Called by DisruptorProducers to publish new exchanges on the RingBuffer
     *
     * @param exchange
     */
    void publish(final Exchange exchange) throws DisruptorNotStartedException {
        disruptorReference.publish(exchange);
    }

    DisruptorReference getDisruptor() {
        return disruptorReference;
    }
}
