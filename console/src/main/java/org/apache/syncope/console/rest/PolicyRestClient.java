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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest Policy services.
 */
@Component
public class PolicyRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    public <T extends AbstractPolicyTO> T getGlobalPolicy(final PolicyType type) {
        T policy = null;
        try {
            policy = getService(PolicyService.class).readGlobal(type);
        } catch (Exception e) {
            LOG.warn("No global " + type + " policy found", e);
        }
        return policy;
    }

    public <T extends AbstractPolicyTO> T getPolicy(final Long id) {
        T policy = null;
        try {
            policy = getService(PolicyService.class).read(id);
        } catch (Exception e) {
            LOG.warn("No policy found for id {}", id, e);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractPolicyTO> List<T> getPolicies(final PolicyType type, final boolean includeGlobal) {
        final List<T> res = new ArrayList<T>();

        try {
            res.addAll((List<T>) getService(PolicyService.class).list(type));
        } catch (Exception ignore) {
            LOG.debug("No policy found", ignore);
        }

        if (includeGlobal) {
            try {
                AbstractPolicyTO globalPolicy = getGlobalPolicy(type);
                if (globalPolicy != null) {
                    res.add(0, (T) globalPolicy);
                }
            } catch (Exception ignore) {
                LOG.warn("No global policy found", ignore);
            }
        }

        return res;
    }

    public <T extends AbstractPolicyTO> void createPolicy(final T policy) {
        getService(PolicyService.class).create(policy);
    }

    public <T extends AbstractPolicyTO> void updatePolicy(final T policy) {
        getService(PolicyService.class).update(policy.getId(), policy);
    }

    public void delete(final Long id, final Class<? extends AbstractPolicyTO> policyClass) {
        getService(PolicyService.class).delete(id);
    }

    public List<String> getCorrelationRuleClasses() {
        List<String> rules = null;

        try {
            rules = CollectionWrapper.unwrap(getService(PolicyService.class).getSyncCorrelationRuleClasses());
        } catch (Exception e) {
            LOG.error("While getting all correlation rule classes", e);
        }

        return rules;
    }
}
