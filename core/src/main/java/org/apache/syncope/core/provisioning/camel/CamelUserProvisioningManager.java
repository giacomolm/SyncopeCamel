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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.sql.rowset.serial.SerialClob;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.camel.CamelContext;
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
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.RouteDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.provisioning.UserProvisioningManager;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CamelUserProvisioningManager implements UserProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(CamelUserProvisioningManager.class);

    private DefaultCamelContext camelContext;

    private RoutesDefinition routes;

    protected Map<String, PollingConsumer> consumerMap;

    protected List<String> knownUri;
            
    @Autowired
    protected RouteDAO routeDao;       

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
        /*ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        return context.getBean("camel-context", DefaultCamelContext.class);*/        
        if(camelContext == null){
            camelContext = new SpringCamelContext(ApplicationContextProvider.getApplicationContext());            
            
            List<CamelRoute> crl = routeDao.findAll();
            InputStream file = getClass().getResourceAsStream("/camelRoute.xml");
                        
            try {
                            
                    DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    JAXBContext jaxbContext = JAXBContext.newInstance(Constants.JAXB_CONTEXT_PACKAGES);                    
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    List rds = new ArrayList();
                    
                    for(int s=0; s<crl.size(); s++){ 
                        
                        //InputStream is = new ByteArrayInputStream(crl.get(s).getRouteContent().getBytes());
                        Document doc = dBuilder.parse(file);                   
                        doc.getDocumentElement().normalize();                    
                        Node routeEl;

                        //ArrayList acl = new ArrayList();                         
                        //NodeList listOfRoutes = doc.getElementsByTagName("route");
                                                                                       
                        routeEl = doc.getElementsByTagName("route").item(0);
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

        return create(userTO, false, null, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, boolean disablePwdPolicyCheck, Boolean enabled,Set<String> excludedResources){
        String uri = "direct:createPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("disablePwdPolicyCheck", disablePwdPolicyCheck);
        props.put("enabled", enabled);
        props.put("excludedResources", excludedResources);

        sendMessage("direct:createUser", userTO, props);

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

        return delete(userId, Collections.<String>emptySet());
    }
    
    @Override
    public List<PropagationStatus> delete(final Long userId, Set<String> excludedResources) {
        String uri = "direct:deletePort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("excludedResources", excludedResources);

        sendMessage("direct:deleteUser", userId,props);

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
    public Map.Entry<Long, List<PropagationStatus>> activate(SyncopeUser user, StatusMod statusMod) {
        String uri = "direct:statusPort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        Map props = new HashMap<String, Object>();
        props.put("token", statusMod.getToken());
        props.put("user", user);
        props.put("statusMod", statusMod);
        
        if (statusMod.isOnSyncope()) {
            sendMessage("direct:activateUser", user.getId(), props);            
        } else {
            WorkflowResult<Long> updated = new WorkflowResult<Long>(user.getId(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:statusUser", updated, props);            
        }               

        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> reactivate(SyncopeUser user, StatusMod statusMod) {
        String uri = "direct:statusPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map props = new HashMap<String, Object>();
        props.put("user", user);
        props.put("statusMod", statusMod);
        
        if (statusMod.isOnSyncope()) {
            sendMessage("direct:reactivateUser", user.getId(),props);
        } else {
            WorkflowResult<Long> updated = new WorkflowResult<Long>(user.getId(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:statusUser", updated, props); 
        }
        
        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> suspend(SyncopeUser user, StatusMod statusMod) {

        String uri = "direct:statusPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map props = new HashMap<String, Object>();
        props.put("user", user);
        props.put("statusMod", statusMod);
        
        if (statusMod.isOnSyncope()) {
            sendMessage("direct:suspendUser", user.getId(),props);
        } else {
            WorkflowResult<Long> updated = new WorkflowResult<Long>(user.getId(), null, statusMod.getType().name().toLowerCase());
            sendMessage("direct:statusUser", updated, props); 
        }
        
        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

        return o.getIn().getBody(Map.Entry.class);
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

    @Override
    public Map.Entry<Long, List<PropagationStatus>> updateInSync(UserMod userMod, Long id, SyncResult result, Boolean enabled, Set<String> excludedResources) {
                
        String uri = "direct:updateSyncPort";
        PollingConsumer pollingConsumer = getConsumer(uri);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("id", id);
        props.put("result", result);
        props.put("enabled", enabled);
        props.put("excludedResources",excludedResources);
        
        sendMessage("direct:updateSyncUser", userMod, props);

        Exchange o = pollingConsumer.receive();
        Exception e;
        if (( e = (Exception) o.getProperty(Exchange.EXCEPTION_CAUGHT)) != null) {
             
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", id, e);

            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            WorkflowResult<Map.Entry<UserMod, Boolean>> updated = new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                    new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
            sendMessage("direct:syncUserStatus", updated, props);
            o = pollingConsumer.receive();
        }

        return o.getIn().getBody(Map.Entry.class);
    }

    @Override
    public void innerSuspend(SyncopeUser user, boolean suspend) {
        
        String uri = "direct:suspendWFPort";
        PollingConsumer pollingConsumer = getConsumer(uri);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("suspend", suspend);

        sendMessage("direct:suspendUserWF", user,props);
        Exchange o = pollingConsumer.receive();

        if (o.getProperty(Exchange.EXCEPTION_CAUGHT) != null) {
            throw (RuntimeException) o.getProperty(Exchange.EXCEPTION_CAUGHT);
        }

    }

}
