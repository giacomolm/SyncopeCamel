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
package org.apache.syncope.core.provisioning.camel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.syncope.core.provisioning.UserProvisioningManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class CamelUserProvisioningManager implements UserProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(CamelUserProvisioningManager.class);

    private DefaultCamelContext camelContext;

    private RoutesDefinition routes;

    protected Map<String, PollingConsumer> consumerMap;

    protected List<String> knownUri;

    public CamelUserProvisioningManager() throws Exception {
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

    protected void sendMessage(String uri, Object obj, Map<String, Object> properties) {
        Exchange exc = new DefaultExchange(getContext());

        Iterator<Map.Entry<String, Object>> it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> property = it.next();
            exc.setProperty(property.getKey(), property.getValue());
            LOG.info("Added property {}", property.getKey());            
        }

        DefaultMessage m = new DefaultMessage();
        m.setBody(obj);
        exc.setIn(m);
        ProducerTemplate template = getContext().createProducerTemplate();
        template.send(uri, exc);
    }

    protected PollingConsumer getConsumer(String uri) {

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
        } else {
            return consumerMap.get(uri);
        }
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO) {
        String uri = "direct:createPort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:createUser", userTO);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);
    }

    /**
     *
     * @param userMod
     * @return
     * @throws RuntimeException if problems arise on workflow update
     */
    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod) {
        String uri = "direct:updatePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:updateUser", userMod);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);
    }

    @Override
    public List<PropagationStatus> delete(final Long userId) {
        String uri = "direct:deletePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:deleteUser", userId);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(final UserMod userMod) {
        String uri = "direct:unlinkPort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:unlinkUser", userMod);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        o.getIn().setBody((o.getIn().getBody(UserMod.class).getId()));
        return o.getIn().getBody(Long.class);
    }

    @Override
    public WorkflowResult<Long> activate(final Long userId, final String token) {
        String uri = "direct:activatePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        Map props = new HashMap<String, Object>();
        props.put("token", token);

        sendMessage("direct:activateUser", userId, props);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(WorkflowResult.class);
    }

    @Override
    public WorkflowResult<Long> reactivate(final Long userId) {
        String uri = "direct:reactivatePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:reactivateUser", userId);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(WorkflowResult.class);
    }

    @Override
    public WorkflowResult<Long> suspend(final Long userId) {

        String uri = "direct:suspendPort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:suspendUser", userId);
        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(WorkflowResult.class);
    }

    @Override
    public Long link(UserMod subjectMod) {
        String uri = "direct:linkPort";
        
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:linkUser", subjectMod);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        o.getIn().setBody((o.getIn().getBody(UserMod.class).getId()));
        return o.getIn().getBody(Long.class);        
    }

    @Override
    public List<PropagationStatus> deprovision(Long user, Collection<String> resources) {
        String uri = "direct:deprovisionPort";
        
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map props = new HashMap<String, Object>();
        props.put("resources", resources);

        sendMessage("direct:deprovisionUser", user, props);
        
        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
        
        return o.getIn().getBody(List.class);               
    }
}
