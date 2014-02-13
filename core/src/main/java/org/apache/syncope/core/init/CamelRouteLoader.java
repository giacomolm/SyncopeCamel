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

package org.apache.syncope.core.init;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.sql.rowset.serial.SerialClob;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.syncope.core.persistence.beans.CamelRoute;
import org.apache.syncope.core.persistence.dao.RouteDAO;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.RouteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class CamelRouteLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(CamelRouteLoader.class);
    
     @Autowired
     private RouteDAO routeDAO;
    
     @Autowired
     private DataSource dataSource;
     
     @Autowired
     protected EntityManager entityManager;
 
 
    @Transactional
    public void load(){
        
        
        //manca la parte del findALL se le rotte sono gia presenti in memoria        
        if(routeDAO.findAll().isEmpty()){
            URL url = getClass().getResource("/camelRoute.xml");                                   

            File file = new File(url.getPath());
            /*String query= "INSERT INTO CamelRoute(ID, NAME, ROUTECONTENT) VALUES (?, ?, ?)";
            try{
                
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

                //try {                 
                
                NodeList listOfRoutes = doc.getElementsByTagName("route");
                for(int s=0; s<listOfRoutes.getLength(); s++){
                    //getting the route node element
                    Node routeEl = listOfRoutes.item(s);
                    //crate an instance of CamelRoute Entity
                    CamelRoute route = new CamelRoute();                                 
                    route.setName(((Element)routeEl).getAttribute("id"));        
                    route.setRouteContent(nodeToString(listOfRoutes.item(s)));
                    //This is the exception cause
                    //routeDAO.save(route);
                    
                    jdbcTemplate.update(query, new Object[]{s+1,((Element)routeEl).getAttribute("id"),  nodeToString(listOfRoutes.item(s))});
                    LOG.error("Route Registration Successed");
                }
            } catch (DataAccessException e) {
                LOG.error("While trying to perform {}", query, e);
            } catch (Exception e) {
                LOG.error("Route Registration failed {}",e.getMessage());
            }*/
            
            try{
                Query q = entityManager.createNativeQuery("INSERT INTO CamelRoute(ID, NAME, ROUTECONTENT) VALUES (?, ?, ?)");
                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();
                

                //try {                 
                
                NodeList listOfRoutes = doc.getElementsByTagName("route");
                for(int s=0; s<listOfRoutes.getLength(); s++){
                    //getting the route node element
                    Node routeEl = listOfRoutes.item(s);
                    //crate an instance of CamelRoute Entity
                    CamelRoute route = new CamelRoute();                                 
                    route.setName(((Element)routeEl).getAttribute("id"));        
                    route.setRouteContent(nodeToString(listOfRoutes.item(s)));
                    //This is the exception cause
                    //routeDAO.save(route);
                    q.setParameter("ID", s+1);
                    q.setParameter("NAME", ((Element)routeEl).getAttribute("id"));
                    q.setParameter("ROUTECONTENT", nodeToString(listOfRoutes.item(s)));
                    q.getResultList();
                    //jdbcTemplate.update(query, new Object[]{s+1,((Element)routeEl).getAttribute("id"),  nodeToString(listOfRoutes.item(s))});
                    LOG.error("Route Registration Successed");
                }
            }
            catch(Exception e){
                
            }
            
        }
    }
    
  private String nodeToString(Node node) {
    StringWriter sw = new StringWriter();
    try{
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        t.transform(new DOMSource(node), new StreamResult(sw));
    }catch (TransformerException te) {
        System.out.println("nodeToString Transformer Exception");
  }
  return sw.toString();
}
}
