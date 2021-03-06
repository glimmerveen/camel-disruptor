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

import org.apache.camel.Exchange;

/**
 * This is a mutable reference to an {@link Exchange}, used as contents of the Disruptors ringbuffer
 */
public class ExchangeEvent {

    private Exchange exchange;

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(final Exchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public String toString() {
        return "ExchangeEvent{" +
                "exchange=" + exchange +
                '}';
    }
}
