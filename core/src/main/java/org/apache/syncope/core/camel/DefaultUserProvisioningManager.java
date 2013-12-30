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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserProvisioningManager implements UserProvisioningManager{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserProvisioningManager.class);

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Autowired
    protected UserDataBinder binder;

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO) {
        WorkflowResult<Map.Entry<Long, Boolean>> created;
        try {
            created = uwfAdapter.create(userTO);
        } catch (RuntimeException e) {
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, userTO.getPassword(), userTO.getVirAttrs());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                created.getResult().getKey(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod) {
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userMod);
        } catch (RuntimeException e) {
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

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                updated.getResult().getKey().getId(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public List<PropagationStatus> delete(final Long userId) {
        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(userId);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        try {
            uwfAdapter.delete(userId);
        } catch (RuntimeException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(UserMod userMod) {
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = uwfAdapter.update(userMod);
        return updated.getResult().getKey().getId();
    }

    @Override
    public Long link(UserMod subjectMod) {
        return uwfAdapter.update(subjectMod).getResult().getKey().getId();
    }
    
    @Override
    public WorkflowResult<Long> activate(final Long userId, final String token) {
        return uwfAdapter.activate(userId, token);
    }

    @Override
    public WorkflowResult<Long> reactivate(final Long userId) {
        return uwfAdapter.reactivate(userId);
    }

    @Override
    public WorkflowResult<Long> suspend(final Long userId) {
        return uwfAdapter.suspend(userId);
    }

    @Override
    public List<PropagationStatus> deprovision(Long userId, Collection<String> resources) {
        
        final SyncopeUser user = binder.getUserFromId(userId);        
        
        final Set<String> noPropResourceName = user.getResourceNames();
        noPropResourceName.removeAll(resources);
        
        final List<PropagationTask> tasks =
                propagationManager.getUserDeleteTaskIds(userId, new HashSet<String>(resources), noPropResourceName);
        final PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        
        return propagationReporter.getStatuses();
    }

}
