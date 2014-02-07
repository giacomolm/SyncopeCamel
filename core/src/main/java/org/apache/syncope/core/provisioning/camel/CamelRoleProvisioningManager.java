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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.camel.model.Constants;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.provisioning.RoleProvisioningManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CamelRoleProvisioningManager implements RoleProvisioningManager{

    private static final Logger LOG = LoggerFactory.getLogger(CamelRoleProvisioningManager.class);

    private DefaultCamelContext camelContext;

    private RoutesDefinition routes;

    protected Map<String, PollingConsumer> consumerMap;

    protected List<String> knownUri;

    public CamelRoleProvisioningManager() throws Exception {
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
        /*ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        return context.getBean("camel-context", DefaultCamelContext.class);*/        
        if(camelContext == null){
            camelContext = new SpringCamelContext(ApplicationContextProvider.getApplicationContext());
            
            InputStream file = getClass().getResourceAsStream("/camelRoute.xml");
                        
            try {
                            
                    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    JAXBContext jaxbContext = JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES);                    
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                    Document doc = dBuilder.parse(file);                   
                    doc.getDocumentElement().normalize();                    
                    Node routeEl;

                    ArrayList acl = new ArrayList();
                    List rds = new ArrayList(); 
                    NodeList listOfRoutes = doc.getElementsByTagName("route");
                    for(int s=0; s<listOfRoutes.getLength(); s++){                                                                    
                        routeEl = doc.getElementsByTagName("route").item(s);
                        JAXBElement  obj = unmarshaller.unmarshal(routeEl, RouteDefinition.class);            
                        //adding route definition to list                        
                        rds.add(obj.getValue());                                
                    }             
                    camelContext.addRouteDefinitions(rds);                
                    camelContext.start();
            } catch (Exception ex) {
                LOG.info("Error during loading camel context {}", ex);
            }
        }
        return camelContext;  
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

    protected PollingConsumer getConsumer(final String uri) {

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
    public Map.Entry<Long, List<PropagationStatus>> create(RoleTO subject) {
        
        return create(subject, Collections.<String>emptySet());        
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(RoleTO roleTO, Set<String> excludedResources) {
        
        String uri = "direct:createRolePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("excludedResources", excludedResources);
        
        sendMessage("direct:createRole", roleTO, props);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> createInSync(RoleTO roleTO, Map<Long, String> roleOwnerMap, Set<String> excludedResources) throws PropagationException {
        
        String uri = "direct:createRoleSyncPort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        Map<String, Object> props = new HashMap<String, Object>();        
        props.put("roleOwnerMap", roleOwnerMap);
        props.put("excludedResources", excludedResources);
           
        sendMessage("direct:createRoleSync", roleTO, props);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {            
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
        
        return o.getIn().getBody(Map.Entry.class);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(RoleMod subjectMod) {
                
        return update(subjectMod, Collections.<String>emptySet());
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(RoleMod subjectMod, Set<String> excludedResources) {

        String uri = "direct:updateRolePort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:updateRole",subjectMod, props);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);        
    }

    @Override
    public List<PropagationStatus> delete(Long subjectId) {
        
        String uri = "direct:deleteRolePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:deleteRole", subjectId);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(List.class);
    }

    @Override
    public Long unlink(RoleMod subjectMod) {
        String uri = "direct:unlinkRolePort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:unlinkRole", subjectMod);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Long.class);
    }

    @Override
    public Long link(RoleMod subjectMod) {
        
        String uri = "direct:linkRolePort";
        
        PollingConsumer pollingConsumer = getConsumer(uri);

        sendMessage("direct:linkRole", subjectMod);

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Long.class);  
    }

    @Override
    public List<PropagationStatus> deprovision(final Long roleId, Collection<String> resources) {
                
        String uri = "direct:deprovisionRolePort";
        
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map props = new HashMap<String, Object>();
        props.put("resources", resources);

        sendMessage("direct:deprovisionRole", roleId, props);
        
        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }
        
        return o.getIn().getBody(List.class);
    }
   
}
