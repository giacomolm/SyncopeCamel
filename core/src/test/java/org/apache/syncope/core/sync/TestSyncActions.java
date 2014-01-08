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
package org.apache.syncope.core.sync;

import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.core.sync.impl.AbstractSyncopeSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

public class TestSyncActions extends DefaultSyncActions {

    private int counter = 0;

    @Override
    public <T extends AbstractAttributableTO> SyncDelta beforeCreate(final AbstractSyncopeSyncResultHandler handler,
            final SyncDelta delta, final T subject) throws JobExecutionException {

        AttributeTO attrTO = null;
        for (int i = 0; i < subject.getAttrs().size(); i++) {
            if ("fullname".equals(subject.getAttrs().get(i).getSchema())) {
                attrTO = subject.getAttrs().get(i);
            }
        }
        if (attrTO == null) {
            attrTO = new AttributeTO();
            attrTO.setSchema("fullname");
            subject.getAttrs().add(attrTO);
        }
        attrTO.getValues().clear();
        attrTO.getValues().add(String.valueOf(counter++));

        return delta;
    }

    @Override
    public <T extends AbstractAttributableTO, K extends AbstractAttributableMod> SyncDelta beforeUpdate(
            final AbstractSyncopeSyncResultHandler handler, final SyncDelta delta, final T subject, final K subjectMod)
            throws JobExecutionException {

        subjectMod.getAttrsToRemove().add("fullname");

        AttributeMod fullnameMod = null;
        for (AttributeMod attrMod : subjectMod.getAttrsToUpdate()) {
            if ("fullname".equals(attrMod.getSchema())) {
                fullnameMod = attrMod;
            }
        }
        if (fullnameMod == null) {
            fullnameMod = new AttributeMod();
            fullnameMod.setSchema("fullname");
            subjectMod.getAttrsToUpdate().add(fullnameMod);
        }

        fullnameMod.getValuesToBeAdded().clear();
        fullnameMod.getValuesToBeAdded().add(String.valueOf(counter++));

        return delta;
    }
}
