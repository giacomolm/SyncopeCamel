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
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;

public interface ProvisioningManager {
    
    public void startContext() throws Exception;
    
    public void stopContext() throws Exception;
    
    public DefaultCamelContext getContext();
    
    public void changeRoute(String routePath);
    
    public Map.Entry<Long, List<PropagationStatus>> createUser(UserTO actual) throws RuntimeException;
    
    public Map.Entry<Long, List<PropagationStatus>> updateUser(UserMod actual) throws RuntimeException;
    
    public List<PropagationStatus> deleteUser(long userId) throws RuntimeException;
    
}
