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
package org.apache.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidatorContext;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.core.connid.ConnPoolConfUtil;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.util.URIUtil;

public class ConnInstanceValidator extends AbstractValidator<ConnInstanceCheck, ConnInstance> {

    @Override
    public boolean isValid(final ConnInstance connInstance, final ConstraintValidatorContext context) {
        boolean isValid = true;

        try {
            URIUtil.buildForConnId(connInstance.getLocation());
        } catch (Exception e) {
            LOG.error("While validating {}", connInstance.getLocation(), e);

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    getTemplate(EntityViolationType.InvalidConnInstanceLocation, e.getMessage())).
                    addNode("location").addConstraintViolation();

            isValid = false;
        }

        if (isValid && connInstance.getPoolConf() != null) {
            try {
                ConnPoolConfUtil.getObjectPoolConfiguration(connInstance.getPoolConf()).validate();
            } catch (Exception e) {
                LOG.error("Invalid pool configuration", e);

                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        getTemplate(EntityViolationType.InvalidConnPoolConf, e.getMessage())).
                        addNode("poolConf").addConstraintViolation();

                isValid = false;
            }
        }

        return isValid;
    }
}
