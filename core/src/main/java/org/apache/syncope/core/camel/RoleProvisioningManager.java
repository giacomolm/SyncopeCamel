/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.syncope.core.camel;

import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.mod.RoleMod;

public interface RoleProvisioningManager extends ProvisioningManager<RoleTO, RoleMod>{
    
    //public List<PropagationStatus> deprovision(final Long roleId, final Collection<String> resources);
    
}
