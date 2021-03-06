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
package org.apache.syncope.core.services;

import java.io.IOException;
import java.io.OutputStream;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.core.rest.controller.WorkflowController;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowServiceImpl extends AbstractServiceImpl implements WorkflowService {

    @Autowired
    private WorkflowController controller;

    @Override
    public Response getOptions(final AttributableType kind) {
        String key = null;
        String value = null;
        switch (kind) {
            case USER:
                key = RESTHeaders.ACTIVITI_USER_ENABLED;
                value = Boolean.toString(ActivitiDetector.isActivitiEnabledForUsers());
                break;

            case ROLE:
                key = RESTHeaders.ACTIVITI_ROLE_ENABLED;
                value = Boolean.toString(ActivitiDetector.isActivitiEnabledForRoles());
                break;

            case MEMBERSHIP:
            default:
        }

        Response.ResponseBuilder builder = Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW);
        if (key != null && value != null) {
            builder.header(key, value);
        }
        return builder.build();
    }

    @Override
    public Response exportDefinition(final AttributableType kind) {
        final MediaType accept =
                messageContext.getHttpHeaders().getAcceptableMediaTypes().contains(MediaType.APPLICATION_JSON_TYPE)
                ? MediaType.APPLICATION_JSON_TYPE
                : MediaType.APPLICATION_XML_TYPE;

        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                switch (kind) {
                    case USER:
                        controller.exportUserDefinition(accept, os);
                        break;

                    case ROLE:
                        controller.exportRoleDefinition(accept, os);
                        break;

                    default:
                        throw new BadRequestException();
                }
            }
        };

        return Response.ok(sout).
                type(accept).
                build();
    }

    @Override
    public Response exportDiagram(final AttributableType kind) {
        StreamingOutput sout = new StreamingOutput() {

            @Override
            public void write(final OutputStream os) throws IOException {
                switch (kind) {
                    case USER:
                        controller.exportUserDiagram(os);
                        break;

                    case ROLE:
                        controller.exportRoleDiagram(os);
                        break;

                    default:
                        throw new BadRequestException();
                }
            }
        };

        return Response.ok(sout).
                type(RESTHeaders.MEDIATYPE_IMAGE_PNG).
                build();
    }

    @Override
    public void importDefinition(final AttributableType kind, final String definition) {
        final MediaType contentType =
                messageContext.getHttpHeaders().getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)
                ? MediaType.APPLICATION_JSON_TYPE
                : MediaType.APPLICATION_XML_TYPE;

        switch (kind) {
            case USER:
                controller.importUserDefinition(contentType, definition);
                break;

            case ROLE:
                controller.importRoleDefinition(contentType, definition);
                break;

            default:
                throw new BadRequestException();
        }
    }
}
