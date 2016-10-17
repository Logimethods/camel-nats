

package io.nats.connector;

import io.nats.client.*;
import io.nats.client.Constants.ConnState;

import java.util.Properties;

import org.slf4j.Logger;

public class NatsConnector implements MessageHandler, Runnable {

    private CamelNatsAdapter 	camelNatsAdapter = null;
    private AsyncSubscription     	subscription = null;
    private Properties       	properties = null;
    
    private Logger            	logger     = null;
    private volatile boolean    running    = false;

    private ConnectionFactory 	connectionFactory = null;
    private Connection        	connection        = null;
	private Object 				threadLock        = null;
	public boolean 			cloudEnvironment = false;

    public NatsConnector(CamelNatsAdapter adapter, Properties props, Logger logger)
    {
        this.camelNatsAdapter = adapter;
        this.properties = props;
        this.logger = logger;
        this.threadLock = new Object();
    }

    class EventHandlers implements ClosedCallback, DisconnectedCallback,
            ExceptionHandler, ReconnectedCallback
    {
        @Override
        public void onReconnect(ConnectionEvent event)
        {           
        	camelNatsAdapter.onReconnect(event);
        }

        @Override
        public void onClose(ConnectionEvent event)
        {
        	camelNatsAdapter.onClose(event);
        }
        
        @Override 
        public void onException(NATSException ex)
        {           
        	logger.error("Asynchronous error: exception: {}",
                        ex.getMessage());

            camelNatsAdapter.onException(ex);        
        }

        @Override
        public void onDisconnect(ConnectionEvent event) {           
        	camelNatsAdapter.onDisconnect(event);          
        }
    }

    @Override
    public void run()
    {
        logger.debug("Setting up NATS Connector.");

        try {
            connectToNats();
        }
        catch (Exception e) {
            logger.error("Setup error: " + e.getMessage());
            logger.debug("Exception: ", e);
            disconnectFromNats();
            return;
        }
        
        camelNatsAdapter.onNatsInitialized();
            
        running = true;

        while (running)
        {
            logger.debug("The NATS Connector is running.");
            synchronized(threadLock)
            {
            	try {
            		threadLock.wait();
            	}
            	catch (InterruptedException e) {
                      
            	}
            }       
        }
        logger.debug("The NATS Connector is exiting.");
        
        disconnectFromNats();
    }
    
    public void shutdown()
    {
    	if (!running)
            return;
    	
      	running = false;
    	
        synchronized (threadLock )
        {
            threadLock.notifyAll();
        }

    	logger.debug("Shutting down NatsConnector");
    }
    
    private void connectToNats() throws Exception
    {
        connectionFactory = new ConnectionFactory(properties);
        EventHandlers eh = new EventHandlers();
        connectionFactory.setClosedCallback(eh);
        connectionFactory.setDisconnectedCallback(eh);
        connectionFactory.setExceptionHandler(eh);
        connectionFactory.setReconnectedCallback(eh);
        if(cloudEnvironment ){
        	connectionFactory.setReconnectAllowed(false);
        }
        connection = connectionFactory.createConnection();
        logger.debug("Connected to NATS cluster.");
    }
    
    public void reconnectCloudToNats(String servers) throws Exception
    {
        //connectionFactory.setServers("nats://127.0.0.1:4333");
        connectionFactory.setServers(servers);
       
        if (subscription != null){
        	subscription = null;
        }
     
        connection = connectionFactory.createConnection();
        logger.debug("Reconnected to URL :" + servers);
    }

    private void disconnectFromNats()
    {
    	camelNatsAdapter.onShutdown();

        try
        {
            if (connection != null)
                 connection.close();
        }
        catch (Exception e) {}

        logger.debug("Closed connection to NATS cluster.");
    }

    public void onMessage(Message m)
    {
        logger.debug("Received Message:" + m.toString());
        
        try
        {
            camelNatsAdapter.onNATSMessage(m);
        }
        catch (Exception e)
        {
            logger.error("Runtime exception thrown by plugin (onMessage): ", e);
        }
    }

    public void publish(Message msg)
    {
        if (!running)
          return;

        try {
            connection.publish(msg);
        }
        catch (Exception ex) {
            logger.error("Exception publishing: " + ex.getMessage());
            logger.debug("Exception: " + ex);
        }
    }

    public void flush() throws Exception
    {
        if ( !running )
            return;

        if (connection == null)
            throw new Exception("Invalid state.  Connection is null.");

        try {
            connection.flush();
        }
        catch (Exception ex)
        {
            throw new Exception("Unable to flush NATS connection.", ex);
        }
    }

    public void subscribe(String subject) throws Exception
    {
        subscribe(subject, null, this);
    }
    
    public void autoUnsubscribe(String subject, int max)
    {
        if (subject == null)
            return;
    
        logger.debug("Plugin unsubscribe after max num of messages from '{}'.", subject);

        if (subscription == null) {
            logger.debug("Subscription not found.");
            return;
        }
        else if(!subscription.getSubject().equalsIgnoreCase(subject)){
            logger.debug("Subscription not found.");
            return;
        }
        else{
	        try {
	        	subscription.autoUnsubscribe(max);
	        } catch (Exception e) {
	            logger.debug("Plugin unsubscribe failed.", e);
	            return;
	        }
        }   
    }
            
    public void subscribe(String subject, String queue, MessageHandler handler) throws Exception {

        if (subject == null)
            return;
        
        if (subscription != null && subscription.getSubject().equalsIgnoreCase(subject)) {
            logger.debug("Subscription already exists.");
            return;
        }

        if (queue == null)
            subscription = connection.subscribeAsync(subject, handler);
        else
        	subscription = connection.subscribeAsync(subject, queue, handler);

        subscription.start();        
    }

    public void subscribe(String subject, String queue) throws Exception {
        subscribe(subject, queue, this);
    }

    public void unsubscribe(String subject)
    {
            
    	logger.debug("Plugin unsubscribe from '{}'.", subject);
    	if (subscription == null || !subscription.getSubject().equalsIgnoreCase(subject)) {
            logger.debug("Subscription not found.");
            return;
        }

        try {
        	subscription.unsubscribe();
        } catch (Exception e) {
            logger.debug("Plugin unsubscribe failed.", e);
            return;
        }

            
        
    }

}
