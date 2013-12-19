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

import org.apache.syncope.common.reqres.PagedResult;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.wrap.ResourceName;

@Path("roles")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface RoleService extends JAXRSService {

    /**
     * @param roleId ID of role to get children from
     * @return Returns list of children for selected role
     */
    @GET
    @Path("{roleId}/children")
    List<RoleTO> children(@PathParam("roleId") Long roleId);

    /**
     * @param roleTO Role to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created role as well as the role itself
     * enriched with propagation status information, as <tt>Entity</tt>
     */
    @POST
    Response create(RoleTO roleTO);

    /**
     * @param roleId ID of role to be deleted
     * @return <tt>Response</tt> object featuring the deleted role enriched with propagation status information,
     * as <tt>Entity</tt>
     */
    @DELETE
    @Path("{roleId}")
    Response delete(@PathParam("roleId") Long roleId);

    /**
     * @return Paged list of all existing roles
     */
    @GET
    PagedResult<RoleTO> list();

    /**
     * @param page result page number
     * @param size number of entries per page
     * @return Paged list of existing roles matching page/size conditions
     */
    @GET
    PagedResult<RoleTO> list(@QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size);

    /**
     * @param roleId Id of role to get parent role from
     * @return Returns parent role or null if no parent exists
     */
    @GET
    @Path("{roleId}/parent")
    RoleTO parent(@PathParam("roleId") Long roleId);

    /**
     * @param roleId ID of role to be read
     * @return Returns role with matching id
     */
    @GET
    @Path("{roleId}")
    RoleTO read(@PathParam("roleId") Long roleId);

    /**
     * @param fiql FIQL search expression
     * @return Paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    PagedResult<RoleTO> search(@QueryParam(PARAM_FIQL) String fiql);

    /**
     * @param fiql FIQL search expression
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    PagedResult<RoleTO> search(@QueryParam(PARAM_FIQL) String fiql, @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @return Paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    PagedResult<RoleTO> search(@QueryParam(PARAM_FIQL) String fiql,
            @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size);

    /**
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    PagedResult<RoleTO> search(@QueryParam(PARAM_FIQL) String fiql,
            @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size,
            @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * This method is similar to {@link #read(Long)}, but uses different authentication handling to ensure that a user
     * can read his own roles.
     *
     * @param roleId ID of role to be read
     * @return Returns role with matching id
     */
    @GET
    @Path("{roleId}/own")
    RoleTO readSelf(@PathParam("roleId") Long roleId);

    /**
     * @param roleId ID of role to be updated
     * @param roleMod Role object containing list of changes to be applied for selected role
     * @return <tt>Response</tt> object featuring the updated role enriched with propagation status information,
     * as <tt>Entity</tt>
     */
    @POST
    @Path("{roleId}")
    Response update(@PathParam("roleId") Long roleId, RoleMod roleMod);

    /**
     * Executes resource-related operations on given role.
     *
     * @param roleId role id.
     * @param type resource association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring the updated role enriched with propagation status information,
     * as <tt>Entity</tt>
     */
    @POST
    @Path("{roleId}/associate/{type}")
    Response associate(@PathParam("roleId") Long roleId, @PathParam("type") ResourceAssociationActionType type,
            List<ResourceName> resourceNames);
}
