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
import java.util.List;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.RoutesDefinition;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CamelProvisioningManager implements ProvisioningManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CamelProvisioningManager.class);

    private DefaultCamelContext camelContext;
    private RoutesDefinition routes;
    private PollingConsumer pollingConsumer;
    List<String> knownUri;

    public CamelProvisioningManager() throws Exception {
        knownUri = new ArrayList<String>();
    }

    @Override
    public void startContext() throws Exception {
        camelContext.start();
    }

    @Override
    public void stopContext() throws Exception {
        camelContext.stop();
    }

    @Override
    public DefaultCamelContext getContext() {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        return context.getBean("camel-context", DefaultCamelContext.class);
    }

    @Override
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

    @Override
    public void sendMessage(String uri, Object obj) {
        Exchange exc = new DefaultExchange(getContext());
        DefaultMessage m = new DefaultMessage();
        m.setBody(obj);
        exc.setIn(m);
        ProducerTemplate template = getContext().createProducerTemplate();
        template.send(uri, exc);
    }

    @Override
    public void startConsumer(String uri) throws Exception {
        if (!knownUri.contains(uri)) {
            knownUri.add(uri);
            Endpoint endpoint = getContext().getEndpoint(uri);
            pollingConsumer = endpoint.createPollingConsumer();
            
            pollingConsumer.start();                        
        }
    }

    @Override
    public void stopConsumer() throws Exception {
        pollingConsumer.stop();
    }

    @Override
    public WorkflowResult createUser() throws RuntimeException{
        Exchange o = pollingConsumer.receive();
        //LOG.info("EXCHANGE BODY 1 {}", o.getProperty(Exchange.EXCEPTION_CAUGHT));
        
        if(o.getProperty(Exchange.EXCEPTION_CAUGHT)!=null)
        {
                throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return (WorkflowResult) o.getIn().getBody();   
    }

}
