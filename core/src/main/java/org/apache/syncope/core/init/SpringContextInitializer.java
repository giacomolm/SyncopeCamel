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

import org.apache.syncope.core.persistence.dao.impl.ContentLoader;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

/**
 * Take care of all initializations needed by Syncope to run up and safe.
 */
@Component
@Configurable
public class SpringContextInitializer implements InitializingBean {

    @Autowired
    private ContentUpgrader contentUpgrader;

    @Autowired
    private ConnectorFactory connFactory;

    @Autowired
    private ContentLoader contentLoader;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private LoggerLoader loggerLoader;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    @Autowired
    private WorkflowAdapterLoader workflowAdapterLoader;
    
    @Autowired
    private CamelRouteLoader routeLoader;

    private boolean upgrade = false;

    public void setUpgrade(final boolean upgrade) {
        this.upgrade = upgrade;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (upgrade) {
            contentUpgrader.upgrade();
        }

        workflowAdapterLoader.load();
        contentLoader.load();
        connFactory.load();
        jobInstanceLoader.load();
        loggerLoader.load();
        classNamesLoader.load();
        
        routeLoader.load();

        workflowAdapterLoader.init();
        
        
    }
}
