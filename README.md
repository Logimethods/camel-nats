# NATS / Camel Component

[Apache Camel](http://camel.apache.org) is a powerful open source integration framework based on known
Enterprise Integration Patterns with powerful Bean Integration.

[NATS messaging system](https://nats.io) (a highly performant cloud native messaging system).

[![MIT License](https://img.shields.io/npm/l/express.svg)](http://opensource.org/licenses/MIT)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Introduction

This component uses [JNATS](https://github.com/nats-io/jnats) java client library version 0.6.0, which requires a JVM 1.8.

The camel-nats component integrates Camel with NATS messaging platform allowing messages to be sent from Camel routes to a NATS Queue or messages to be consumed from a NATS Queue or Topic by Camel routes.

This camel-nats component could be used for stand-alone camel deployment (for example using org.apache.camel.spring.Main), for osgi camel deployment (for example using apache karaf) and for cloud deployment using Apcera cloud platform.

# Installation

Maven users will need to add the following dependency to their pom.xml for this component:

```
<dependency>
    <groupId>com.logimethods.apcera</groupId>
    <artifactId>camel-nats</artifactId>
    <version>2.18-SNAPSHOT</version>
</dependency>

```

The latest release uses camel-core version 2.18-SNAPSHOT as dependency.
Download component and build it using mvn install command from the top component source directory containing pom.xml 

# URI format

For non cloud deployments (standalone, karaf):

```

nats:servers[?options]

```
Where "servers" represents the list of NATS servers.

For cloud deployments (only Apcera is currently supported):

```

nats:APCERA:JobLinkName[?options]

```
Where "JobLinkName" represents the name of Apcera link between Camel job and NATS job (or service)

# Options

NATS endpoints support the following options, depending on whether they are acting like a Producer or as a Consumer. The options are consistent with the "official" camel-nats component described here [Apache camel-nats](http://camel.apache.org/nats.html

| Option              	  |	Default   | Description                                                                        
|-------------------------|-----------|-------------------------------------------------------------------------------------------------|
| topic                   | null      | The topic to subscribe/publish to.																|
| reconnect               | true      | Whether or not to use the reconnection feature.													|
| pedantic                | false     | Whether or not running in pedantic mode (this affects performance).								|
| verbose                 | false     | Whether or not running in verbose mode															|
| ssl                     | false     | Whether or not to use SSL																		|
| reconnectTimeWait       | 2000      | Waiting time before attempts reconnection (in milliseconds)										|
| maxReconnectAttempts    | 3         | Set the maximum number of reconnection attempts in case the connection is lost.					|
| pingInterval            | 4000      | Ping interval to be aware if connection is still alive (in milliseconds)						|
| noRandomizeServers      | false     | Whether or not to randomize the order of servers for the connection attempts					|
| queueName               | null      | The Queue name if we are using NATS for a queue configuration (consumer).						|
| maxMessages             | null      | Stop receiving messages from a topic we are subscribing to after maxMessages (consumer).		|
| poolSize                | 10        | Pool size for consumer workers (consumer).														|



# Examples

# Code Examples

# TODO List


## License

(The MIT License)

Copyright (c) 2016 Logimethods.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to
deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
