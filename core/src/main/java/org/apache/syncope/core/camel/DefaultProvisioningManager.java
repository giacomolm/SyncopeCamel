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
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RoutesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultProvisioningManager implements ProvisioningManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultProvisioningManager.class);

    @Autowired
    private ModelCamelContext context;

    private RoutesDefinition routes;

    public DefaultProvisioningManager() throws Exception {
        //context = new DefaultCamelContext();
        //loading default route
        InputStream is = getClass().getResourceAsStream("/camelRoute.xml");

        try {
            routes = context.loadRoutesDefinition(is);
            context.addRouteDefinitions(routes.getRoutes());

        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }
        //startContext();

    }

    @Override
    public void startContext() throws Exception {
        context.start();
    }

    @Override
    public void stopContext() throws Exception {
        context.stop();
    }

    @Override
    public ModelCamelContext getContext() {
        return context;
    }

    @Override
    public void changeRoute(String routePath) {
        try {
            context.removeRouteDefinitions(routes.getRoutes());
            InputStream is = getClass().getResourceAsStream(routePath);
            routes = getContext().loadRoutesDefinition(is);
            context.addRouteDefinitions(routes.getRoutes());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
