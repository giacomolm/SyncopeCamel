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
package org.apache.syncope.common.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.RESTHeaders;

@Path("workflows/{kind}")
public interface WorkflowService extends JAXRSService {

    /**
     * @param kind Kind can be USER or ROLE only!
     * @return Response contains special syncope HTTP header indicating if Activiti is enabled for users / roles
     * @see org.apache.syncope.common.types.RESTHeaders#ACTIVITI_USER_ENABLED
     * @see org.apache.syncope.common.types.RESTHeaders#ACTIVITI_ROLE_ENABLED
     */
    @OPTIONS
    Response getOptions(@PathParam("kind") AttributableType kind);

    /**
     * @param kind Kind can be USER or ROLE only!
     * @return Returns workflow definition for matching kind.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response exportDefinition(@PathParam("kind") AttributableType kind);

    @GET
    @Path("diagram.png")
    @Produces({ RESTHeaders.MEDIATYPE_IMAGE_PNG })
    Response exportDiagram(@PathParam("kind") AttributableType kind);

    /**
     * @param kind Kind can be USER or ROLE only!
     * @param definition workflow definition for matching kind
     */
    @PUT
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void importDefinition(@PathParam("kind") AttributableType kind, String definition);
}
