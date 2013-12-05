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
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DefaultProvisioningManager implements ProvisioningManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultProvisioningManager.class);

    private SpringCamelContext camelContext;
    private RoutesDefinition routes;
    private PollingConsumer pollingConsumer;
    List<String> knownUri;

    public DefaultProvisioningManager() throws Exception {
        
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        camelContext = new SpringCamelContext(context);
        knownUri = new ArrayList<String>();

        try {
            InputStream is = getClass().getResourceAsStream("/camelRoute.xml");
            routes = camelContext.loadRoutesDefinition(is);
            camelContext.addRouteDefinitions(routes.getRoutes());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOG.error("Unexpected error", e);
        }
        camelContext.start();
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
    public ModelCamelContext getContext() {
        return camelContext;
    }

    @Override
    public void changeRoute(String routePath) {
        try {
            //InputStream is = getClass().getResourceAsStream("/camelRoute.xml");
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
        if(!knownUri.contains(uri)){
            knownUri.add(uri);
            Endpoint endpoint = getContext().getEndpoint(uri);
            pollingConsumer = endpoint.createPollingConsumer();
            pollingConsumer.start();
        }
    }

    @Override
    public Object getMessage(Class type) throws Exception {
        Exchange o = pollingConsumer.receive();              
        return o.getIn().getBody();
    }

}
