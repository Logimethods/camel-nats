package io.nats.connector;

import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.camel.component.nats.NatsConstants;
import org.apache.camel.component.nats.NatsConsumer;
import org.apache.camel.component.nats.NatsProducer;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;

import io.nats.client.ConnectionEvent;
import io.nats.client.Message;
import io.nats.client.NATSException;
import io.nats.client.Subscription;

public class CamelNatsAdapter {
	
	private NatsConnector natsConnector;
	private Logger logger;	
	private NatsConsumer natsConsumer = null;
	private NatsProducer natsProducer = null;
	private Subscription sid;
	
	enum AdapterType{
		PRODUCER,
		CONSUMER
	};
	AdapterType adapterType;
	
	public CamelNatsAdapter(NatsConsumer natsConsumer, Properties natsProperties, Logger logger) {
		this.natsConsumer = natsConsumer;
		this.adapterType = AdapterType.CONSUMER;
		natsConnector = new NatsConnector(this, natsProperties, logger);
		natsConnector.cloudEnvironment = 
				natsConsumer.getEndpoint().getNatsConfiguration().isCloudEnvironment();
		this.logger = logger;
	}

	public CamelNatsAdapter(NatsProducer natsProducer, Properties natsProperties, Logger logger) {
		this.natsProducer = natsProducer;
		this.logger = logger;
		this.adapterType = AdapterType.PRODUCER;
		this.natsConnector = new NatsConnector(this, natsProperties, logger);
		natsConnector.cloudEnvironment = 
				natsProducer.getEndpoint().getNatsConfiguration().isCloudEnvironment();
	}
	
	public boolean onNatsInitialized() {	
		
		if(adapterType == AdapterType.PRODUCER){
			logger.debug("Received NATS producer onInitialized event");
			if (natsProducer.getStartupLatch() != null)
				natsProducer.getStartupLatch().countDown();
		}
		else if(adapterType == AdapterType.CONSUMER){
			logger.debug("Received NATS consumer onInitialized event");
			subscribe();	        
	        if (natsConsumer.getStartupLatch() != null)
	        	natsConsumer.getStartupLatch().countDown();
		}			
	    return true;
	}
	
	private void subscribe(){
		  try
	        {
	        	if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getQueueName())) {
	        		natsConnector.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
	        				natsConsumer.getEndpoint().getNatsConfiguration().getQueueName());
	        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
	        			natsConnector.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
	        					Integer.parseInt(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages()));
	        		}
	        	}
	        	else{
	        		natsConnector.subscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
	        		if (ObjectHelper.isNotEmpty(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages())) {
	        			natsConnector.autoUnsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic(),
	        					Integer.parseInt(natsConsumer.getEndpoint().getNatsConfiguration().getMaxMessages()));
	        		}
	        	}
	        }
	        catch (Throwable e) {
	        	logger.error("Unable to subscribe");
	        	natsConsumer.getExceptionHandler().handleException("Error during processing", e);
	        }
	}
	
	public void onNATSMessage(Message msg) {
				
		logger.debug("Received NATS message: " + msg.toString());
		
		Exchange exchange = natsConsumer.getEndpoint().createExchange();
        exchange.getIn().setBody(msg);
        exchange.getIn().setHeader(NatsConstants.NATS_MESSAGE_TIMESTAMP, System.currentTimeMillis());
        exchange.getIn().setHeader(NatsConstants.NATS_SUBSCRIPTION_ID, sid);
        try {
       	 natsConsumer.getProcessor().process(exchange);
        } catch (Exception e) {
       	 natsConsumer.getExceptionHandler().handleException("Error during processing", exchange, e);
        }	
	}

	void onClose(ConnectionEvent event) {
		
		if(adapterType == AdapterType.PRODUCER){	    	     
			if(natsProducer.getShutdownLatch() != null)
				natsProducer.getShutdownLatch().countDown();
		 }
		else if(adapterType == AdapterType.CONSUMER){	    	     
			if(natsConsumer.getShutdownLatch() != null)
				natsConsumer.getShutdownLatch().countDown();
		 }			
	}

	public void onShutdown() {	
		
	     if(natsConnector == null)
	    	 return;
	     
	     if(adapterType == AdapterType.CONSUMER){	
	    	 logger.debug("Shutting down NatsConnector (Consumer)");
		     try {
		    	 natsConnector.unsubscribe(natsConsumer.getEndpoint().getNatsConfiguration().getTopic());
		     } catch (Exception e) {
		    	 natsConsumer.getExceptionHandler().handleException("Error during unsubscribing", e);
		     }	
	     }
	     else if(adapterType == AdapterType.PRODUCER){	    	        
	    	 logger.debug("Shutting down NatsConnector (Producer): ");
	     }
	}

	public void publish(String subject, String replySubject, byte[] payload) throws Exception {
		if (natsConnector == null){
			logger.error("NATS connection is not initialized");
		    throw new Exception("Invalid State Nats Connector is null");
		}      
		
		Message m = new Message();
		m.setSubject(subject);
	        
		if (replySubject != null)
	        m.setReplyTo(replySubject);
	        
		m.setData(payload, 0, payload.length);
	
		natsConnector.publish(m);
	
		try {
			natsConnector.flush();
	    }
	    catch (Exception e){
	       	logger.error("Error with flush:  ");
	       	throw e;
	    }		
	}

	public void onReconnect(ConnectionEvent event) {
	 	logger.debug("Adapter Reconnected ", event.toString());		
	}

	public void onException(NATSException ex) {
		logger.debug("Adapter Exception ", ex.toString());	
		

		if(adapterType == AdapterType.PRODUCER){	    	     
			// Need to notify camel framework here
		 }
		else if(adapterType == AdapterType.CONSUMER){	    	     
			natsConsumer.getExceptionHandler().handleException("Error during processing", ex);
		 }			

	}

	public void onDisconnect(ConnectionEvent event) {
		
		if(adapterType == AdapterType.PRODUCER){	
			logger.debug("Producer Disconnected ", event.toString());	
			if(natsProducer.getEndpoint().getNatsConfiguration().isCloudEnvironment()){
				logger.debug("Cloud Producer Disconnected ", event.toString());	
				try {
					String natsServer = System.getenv(natsProducer.getEndpoint().getNatsConfiguration().getCloudURI()).replace("tcp:","nats:");
					logger.debug("Attempting to reconnect producer to NATS with nats URI: " + natsServer);
					natsConnector.reconnectCloudToNats(natsServer);
					logger.debug("Producer connected");
				} catch (Exception e) {
					logger.error("Producer Can't connect to NATS");
					e.printStackTrace();
				}
			}
		 }
		else if(adapterType == AdapterType.CONSUMER){	
			logger.debug("Consumer Disconnected ", event.toString());	
			if(this.natsConsumer.getEndpoint().getNatsConfiguration().isCloudEnvironment()){
				logger.debug("Cloud Consumer Disconnected ", event.toString());	
				try {
					String natsServer = System.getenv(natsConsumer.getEndpoint().getNatsConfiguration().getCloudURI());
					natsServer.replace("tcp:","nats:");
					logger.debug("Attempting to reconnect consumer to NATS with nats URI: " + natsServer);
					natsConnector.reconnectCloudToNats(natsServer);
					logger.debug("Consumer connected");
					subscribe();
				} catch (Exception e) {
					logger.error("Consumer Can't connect to NATS");
					e.printStackTrace();
				}
				
			}
		 }		
		
	}

	public void shutdown() {
		this.natsConnector.shutdown();		
	}

	public Runnable getConnector() {		
		return natsConnector;
	}		
}

