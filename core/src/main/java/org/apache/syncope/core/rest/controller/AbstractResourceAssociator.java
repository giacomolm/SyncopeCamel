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

import java.util.Collection;
import org.apache.syncope.common.to.AbstractAttributableTO;

public abstract class AbstractResourceAssociator<T extends AbstractAttributableTO> extends AbstractController<T> {

    public abstract T unlink(Long id, Collection<String> resources);

    public abstract T link(Long id, Collection<String> resources);

    public abstract T unassign(Long id, Collection<String> resources);

    public abstract T assign(Long id, Collection<String> resources, boolean changepwd, String password);

    public abstract T deprovision(Long userId, Collection<String> resources);

    public abstract T provision(Long userId, Collection<String> resources, boolean changepwd, String password);
}
