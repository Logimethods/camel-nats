
package org.apache.camel.component.nats;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

//@Ignore("Require a running Nats server")
public class ApceraEnvProducerTest extends CamelTestSupport {
    
    @Test
    public void sendTest() throws Exception {
    	
    	//This required environment variable "NATS_URI" to be set on test machine
    	//For examle: NATS_URI = tcp://0.0.0.0:4222
    	String url = System.getenv("NATS_URI");
    	System.out.println("Environment var is: " + url);
    	
        template.sendBody("direct:send", "pippo");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:send").to("nats://APCERA:NATS?topic=test");
            }
        };
    }
}

