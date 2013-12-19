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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.wrap.EntitlementTO;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.util.EntitlementUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntitlementController extends AbstractTransactionalController<EntitlementTO> {

    @Autowired
    private EntitlementDAO entitlementDAO;

    public List<String> getAll() {
        List<Entitlement> entitlements = entitlementDAO.findAll();
        List<String> result = new ArrayList<String>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            result.add(entitlement.getName());
        }

        return result;
    }

    public Set<String> getOwn() {
        return EntitlementUtil.getOwnedEntitlementNames();
    }

    @Override
    protected EntitlementTO resolveReference(Method method, Object... args) throws UnresolvedReferenceException {
        throw new UnresolvedReferenceException();
    }
}
