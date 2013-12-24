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

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.core.workflow.WorkflowResult;

public interface ProvisioningManager<T extends AbstractAttributableTO, M extends AbstractAttributableMod> {

    public Map.Entry<Long, List<PropagationStatus>> create(T subject);

    public Map.Entry<Long, List<PropagationStatus>> update(M subjectMod);

    public List<PropagationStatus> delete(Long subjectId);

    public UserMod unlink(M subjectMod);

    public WorkflowResult<Long> activate(Long subjectId, String token);

    public WorkflowResult<Long> reactivate(Long subjectId);

    public WorkflowResult<Long> suspend(Long subjectId);

}
