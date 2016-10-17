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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

public class NatsComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        
    	NatsConfiguration config = new NatsConfiguration();
        setProperties(config, parameters);
        String natsServer = remaining;
        
        //Determine if NATS server is deployed as stand-alone server or as Apcera service
        if(remaining.substring(0,7).equalsIgnoreCase("Apcera:") == true){
            //Handle Apcera specific URI through service binding
        	String cloudUri = remaining.substring(7).toUpperCase() + "_URI";
        	natsServer = System.getenv(cloudUri).replace("tcp://","");
        	config.setCloudEnvironment(true);
        	config.setCloudURI(cloudUri);
        	config.setReconnect(false);
        }   
       
        config.setServers(natsServer);
        NatsEndpoint endpoint = new NatsEndpoint(uri, this, config);
        return endpoint;
    }

}
