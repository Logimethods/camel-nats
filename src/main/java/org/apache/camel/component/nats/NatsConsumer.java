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

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.nats.connector.CamelNatsAdapter;

import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NatsConsumer extends DefaultConsumer {

    private static Logger logger = LoggerFactory.getLogger(NatsConsumer.class);

    private ExecutorService executor;    
    private CountDownLatch startupLatch = null;
    private CountDownLatch shutdownLatch = null;				

    private CamelNatsAdapter natsAdapters[] = null;
    private int poolSize;

    public NatsConsumer(NatsEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    public NatsEndpoint getEndpoint() {
        return (NatsEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStart() throws Exception {
    	
        super.doStart();
        logger.info("Starting Nats Consumer");

        NatsConfiguration config = getEndpoint().getNatsConfiguration();       	 
   	 	setStartupLatch(new CountDownLatch(config.getPoolSize()));  	 	
   	 	Properties natsProperties = getEndpoint().getNatsConfiguration().createProperties();
   	 	executor = getEndpoint().createConsumerExecutor();
   	 	poolSize = getEndpoint().getNatsConfiguration().getPoolSize();
   	 	natsAdapters = new CamelNatsAdapter[poolSize];
   	 	
   	 	for (short i = 0; i < poolSize; i++){
   	 		natsAdapters[i] = new CamelNatsAdapter(this, natsProperties, logger);                  	 	
	   	 	executor.submit((Runnable)natsAdapters[i].getConnector());   
   	 	}
   	   	 	  	 	
   	 	// Wait for connector to fully initialize
        boolean initialized = true;
        try{
        	//initialized = startupLatch.await(10, TimeUnit.SECONDS);
        	getStartupLatch().await();
        }
        catch(InterruptedException e){
        	logger.error("Nats consumer initilization was interrupted"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        
        if (initialized == false){
        	logger.info("Nats Consumer initilization is taking longer then expected"); 
        	throw new Exception("Startup failure in NATS Connector");
        }
        
        logger.info("Started NATS Consumer");
    }

    @Override
    protected void doStop() throws Exception {
    	 logger.debug("Stopping Nats Consumer");
    	 super.doStop();
    	 
    	 setShutdownLatch(new CountDownLatch(poolSize));
    	
    	 for (short i = 0; i < poolSize; i++){	   	
	         if (natsAdapters[i] != null) {
	        	 natsAdapters[i].shutdown();
	         }
    	 }
         
         boolean shutdown = true;
         try{
         	//shutdown = shutdownLatch.await(10, TimeUnit.SECONDS);
        	 shutdownLatch.await();
         }
         catch(InterruptedException e){
         	logger.error("Nats consumer shutdown was interrupted"); 
         }
         
         if (shutdown == false){
         	logger.error("Nats Consumer shutdown timed out"); 
         }
                 
         if (executor != null) {
            if (getEndpoint() != null && getEndpoint().getCamelContext() != null) {
                getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(executor);
            } else {
                executor.shutdownNow();
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
	
	