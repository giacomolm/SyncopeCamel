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

package org.apache.syncope.core.camel.processors;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserPropagation implements Processor{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserPropagation.class);
    
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Override
    public void process(Exchange exchange){
      
        if((exchange.getIn().getBody() instanceof WorkflowResult)){
            
            WorkflowResult<Map.Entry<Long, Boolean>> created = (WorkflowResult) exchange.getIn().getBody();            
            UserTO actual = exchange.getProperty("actual", UserTO.class);
            
            
            List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                    created, actual.getPassword(), actual.getVirAttrs());
            PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                    getBean(PropagationReporter.class);
            try {
                taskExecutor.execute(tasks, propagationReporter);
            } catch (PropagationException e) {
                LOG.error("Error propagation primary resource {}", e);
                propagationReporter.onPrimaryResourceFailure(tasks);
            }
            
            Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(created.getResult().getKey(), propagationReporter.getStatuses());         
            exchange.getOut().setBody(result);
        }               
    }
    
    
}
