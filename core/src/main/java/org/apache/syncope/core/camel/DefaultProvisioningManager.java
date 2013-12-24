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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultProvisioningManager implements ProvisioningManager{
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultProvisioningManager.class);

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;
    
    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> createUser(UserTO actual) throws RuntimeException {
        
        WorkflowResult<Map.Entry<Long, Boolean>> created;
        try{
            created = uwfAdapter.create(actual);
        }
        catch(RuntimeException e){
            throw e;
        }
        
        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, actual.getPassword(), actual.getVirAttrs());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        
        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(created.getResult().getKey(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> updateUser(UserMod actual) throws RuntimeException {
        
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try{
            updated = uwfAdapter.update(actual);
        }
        catch(RuntimeException e){
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        
        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(updated.getResult().getKey().getId(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public List<PropagationStatus> deleteUser(long userId) throws RuntimeException {
        
        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(userId);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        
        try{
            uwfAdapter.delete(userId);
        }
        catch(RuntimeException e){
            throw e;
        }
        
        return propagationReporter.getStatuses();
    }

    @Override
    public UserMod unlinkUser(UserMod userMod) throws RuntimeException {
       
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try{
             updated = uwfAdapter.update(userMod);
        }
        catch(RuntimeException e){
            throw e;
        }
        
        return updated.getResult().getKey();
    }

    @Override
    public WorkflowResult<Long> activateUser(Long userId, String token) throws RuntimeException {
         WorkflowResult<Long> updated;
         try{
             updated = uwfAdapter.activate(userId, token);
         }
         catch(RuntimeException e){
             throw e;
         }
         
         return updated;
    }

    @Override
    public WorkflowResult<Long> reactivateUser(Long userId) throws RuntimeException {
        WorkflowResult<Long> updated;
         try{
             updated = uwfAdapter.reactivate(userId);
         }
         catch(RuntimeException e){
             throw e;
         }
         return updated;
    }

    @Override
    public WorkflowResult<Long> suspendUser(Long userId) throws RuntimeException {
        WorkflowResult<Long> updated;
         try{
             updated = uwfAdapter.suspend(userId);
         }
         catch(RuntimeException e){
             throw e;
         }
         
         return updated;
    }
    
}
