/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.nats;


import io.nats.connector.CamelNatsAdapter;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsProducer extends DefaultProducer{
    
    private static final Logger logger = LoggerFactory.getLogger(NatsProducer.class);
    private CountDownLatch 		startupLatch = null;
    private CountDownLatch 		shutdownLatch = null;
    
    private CamelNatsAdapter 	natsAdapter = null;
	private ExecutorService 	executor = null;
    
    public NatsProducer(NatsEndpoint endpoint) {
        super(endpoint);   
    }
    
    @Override 
    public NatsEndpoint getEndpoint() {
        return (NatsEndpoint) super.getEndpoint();
    }
    
    @Override 
    public void process(Exchange exchange) throws Exception {
        NatsConfiguration config = getEndpoint().getNatsConfiguration();
        String body = exchange.getIn().getMandatoryBody(String.class);

        logger.info("Publishing to topic: {}", config.getTopic());
        
       
        String replySubject = config.getReplySubject();
           
        if (ObjectHelper.isNotEmpty(config.getReplySubject())) {
            publish(config.getTopic(), replySubject, body.getBytes());
        } else {
        	publish(config.getTopic(), null, body.getBytes());
        }
    }
    
    public void publish(String subject, String replySubject, byte[] payload) throws Exception{		
    	natsAdapter.publish(subject, replySubject, payload);
	}
    
    @Override 
    protected void doStart() throws Exception {
        super.doStart();
        logger.debug("Starting Nats Producer");    
        startupLatch = new CountDownLatch(1);  
        
        Properties natsProperties = getEndpoint().getNatsConfiguration().createProperties();
        natsAdapter = new CamelNatsAdapter(this, natsProperties, logger);            
        executor = getEndpoint().createProducerExecutor();
        executor.submit((Runnable)natsAdapter.getConnector());      
       
        // Wait for connector to fully initialize
        boolean initialized = true;
        try{
        	//initialized = startupLatch.await(10, TimeUnit.SECONDS);
        	startupLatch.await();
        }
        catch(InterruptedException e){
        	logger.error("Nats Producer initilization was interrupted"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        
        if (initialized == false){
        	logger.info("Nats Producer initilization is taking longer then expected"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        
        logger.info("Started NATS Producer");
    }

    @Override  
    protected void doStop() throws Exception {
       
    	 logger.info("Stopping Nats Producer");
    	 super.doStop();
        
        shutdownLatch = new CountDownLatch(1);
        //connector.shutdown(); 
        natsAdapter.shutdown();
        boolean shutdown = false;
        try{
        	shutdown = shutdownLatch.await(10, TimeUnit.SECONDS);
        	//shutdownLatch.await();
        }
        catch(InterruptedException e){
        	logger.error("Nats Producer shutdown was interrupted"); 
        }
        
        if (shutdown == false){
        	logger.error("Nats Producer shutdown timed out"); 
        }
        
        if (executor != null) {
            if (getEndpoint() != null && getEndpoint().getCamelContext() != null) {
                getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(executor);
            } else {
                executor.shutdown();
            }
         }
         executor = null;           
    }

    public CountDownLatch getStartupLatch() {
		return startupLatch;
	}

	public void setStartupLatch(CountDownLatch startupLatch) {
		this.startupLatch = startupLatch;
	}

	public CountDownLatch getShutdownLatch() {
		return shutdownLatch;
	}

	public void setShutdownLatch(CountDownLatch shutdownLatch) {
		this.shutdownLatch = shutdownLatch;
	}
  

	

}
