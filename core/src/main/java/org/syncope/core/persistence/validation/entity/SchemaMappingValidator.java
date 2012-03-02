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
package org.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.util.SchemaMappingUtil;
import org.syncope.types.EntityViolationType;

public class SchemaMappingValidator extends AbstractValidator
        implements ConstraintValidator<SchemaMappingCheck, SchemaMapping> {

    @Override
    public void initialize(final SchemaMappingCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(final SchemaMapping mapping, final ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        if (StringUtils.isBlank(SchemaMappingUtil.getExtAttrName(mapping))) {
            context.buildConstraintViolationWithTemplate("Missing external attribute name").
                    addNode(EntityViolationType.InvalidSchemaMapping.toString()).addConstraintViolation();

            return false;
        }

        if (StringUtils.isBlank(SchemaMappingUtil.getIntAttrName(mapping))) {
            context.buildConstraintViolationWithTemplate("Missing internal attribute name").
                    addNode(EntityViolationType.InvalidSchemaMapping.toString()).addConstraintViolation();

            return false;
        }

        return true;
    }
}
