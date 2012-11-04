camel-disruptor
===============

Camel component for the [LMAX disruptor](https://github.com/LMAX-Exchange/disruptor)

This component is still under heavy development, but the goal is to provide a component that is functionally comparable
to the standard[Camel SEDA component](http://camel.apache.org/seda.html) but based on a higher performance Disruptor
instead of a standard blocking queue.