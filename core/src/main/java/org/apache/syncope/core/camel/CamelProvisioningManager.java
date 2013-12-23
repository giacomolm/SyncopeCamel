/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.camel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.RoutesDefinition;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CamelProvisioningManager implements ProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(CamelProvisioningManager.class);

    private DefaultCamelContext camelContext;
    private RoutesDefinition routes;
    
    protected Map<String,PollingConsumer> consumerMap;
    protected List<String> knownUri;

    public CamelProvisioningManager() throws Exception {
        knownUri = new ArrayList<String>();
        consumerMap = new HashMap();
    }

    public void startContext() throws Exception {
        camelContext.start();
    }

    public void stopContext() throws Exception {
        camelContext.stop();
    }

    public DefaultCamelContext getContext() {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        return context.getBean("camel-context", DefaultCamelContext.class);
    }

    public void changeRoute(String routePath) {
        try {
            camelContext.removeRouteDefinitions(routes.getRoutes());
            InputStream is = getClass().getResourceAsStream(routePath);
            routes = getContext().loadRoutesDefinition(is);
            camelContext.addRouteDefinitions(routes.getRoutes());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error("Unexpected error", e);
        }
    }

    protected void sendMessage(String uri, Object obj) {
        Exchange exc = new DefaultExchange(getContext());
        DefaultMessage m = new DefaultMessage();
        m.setBody(obj);
        exc.setIn(m);
        ProducerTemplate template = getContext().createProducerTemplate();
        template.send(uri, exc);
    }
    
    protected PollingConsumer getConsumer(String uri){        
                
        if (!knownUri.contains(uri)) {
            knownUri.add(uri);
            Endpoint endpoint = getContext().getEndpoint(uri);
            PollingConsumer pollingConsumer = null;
            try {
                pollingConsumer = endpoint.createPollingConsumer();
                consumerMap.put(uri, pollingConsumer);
                pollingConsumer.start();
            } catch (Exception ex) {
                 LOG.error("Unexpected error in Consumer creation ", ex);
            }
            return pollingConsumer;
        }
        else{
            return consumerMap.get(uri);
        }
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> createUser(UserTO actual) throws RuntimeException{
            
        String uri = "direct:createPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        sendMessage("direct:createUser", actual);      
        
        Exchange o = pollingConsumer.receive();
        
        if(o.getProperty(Exchange.EXCEPTION_CAUGHT)!= null)
        {
                throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);   
    }

     /**
      *
      * @param actual
      * @return 
      * @throws RuntimeException if problems arise on workflow update
      */
    @Override
    public Map.Entry<Long, List<PropagationStatus>> updateUser(UserMod actual) throws RuntimeException{

        String uri = "direct:updatePort";
        PollingConsumer pollingConsumer= getConsumer(uri);
        
        sendMessage("direct:updateUser", actual);      

        Exchange o = pollingConsumer.receive();

        if(o.getProperty(Exchange.EXCEPTION_CAUGHT)!= null){
                throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);   
     }

    @Override
    public List<PropagationStatus> deleteUser(long userId) throws RuntimeException {
        
        String uri = "direct:deletePort";
        PollingConsumer pollingConsumer= getConsumer(uri);
        
        sendMessage("direct:deleteUser", userId);
        
        Exchange o = pollingConsumer.receive();
        
        if(o.getProperty(Exchange.EXCEPTION_CAUGHT)!= null){
                throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
        
        return o.getIn().getBody(List.class);   
    }
    
    @Override
    public UserMod unlinkUser(UserMod userMod) throws RuntimeException {
        
        String uri = "direct:unlinkPort";
        PollingConsumer pollingConsumer= getConsumer(uri);
        
        sendMessage("direct:unlinkUser", userMod);
        
        Exchange o = pollingConsumer.receive();
        
        if(o.getProperty(Exchange.EXCEPTION_CAUGHT)!= null){
                throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
        
        return o.getIn().getBody(UserMod.class);   
    }

}
