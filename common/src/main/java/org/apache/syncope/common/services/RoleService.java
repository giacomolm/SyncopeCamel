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

import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.ResourceNameTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;

@Path("roles")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface RoleService {

    /**
     * @param roleId ID of role to get children from
     * @return Returns list of children for selected role
     */
    @GET
    @Path("{roleId}/children")
    List<RoleTO> children(@PathParam("roleId") Long roleId);

    /**
     * @return Returns number of known roles. (size of list)
     */
    @GET
    @Path("count")
    int count();

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
     * @return Returns list of all knwon roles
     */
    @GET
    List<RoleTO> list();

    /**
     * @param page Page of roles in relation to size parameter
     * @param size Number of roles to be displayed per page
     * @return Returns paginated list of roles
     */
    @GET
    List<RoleTO> list(@QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("25") int size);

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
     * @param searchCondition Filter condition for role list
     * @return Returns list of roles with matching filter conditions
     * @throws InvalidSearchConditionException if given search condition is not valid
     */
    @POST
    @Path("search")
    List<RoleTO> search(NodeCond searchCondition) throws InvalidSearchConditionException;

    /**
     * @param searchCondition Filter condition for role list
     * @param page Page of roles in relation to size parameter
     * @param size Number of roles to be displayed per page
     * @return Returns paginated list of roles with matching filter conditions
     * @throws InvalidSearchConditionException if given search condition is not valid
     */
    @POST
    @Path("search")
    List<RoleTO> search(NodeCond searchCondition, @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("25") int size) throws InvalidSearchConditionException;

    /**
     * @param searchCondition Filter condition for role list
     * @return Returns number of roles matching provided filter conditions
     * @throws InvalidSearchConditionException if given search condition is not valid
     */
    @POST
    @Path("search/count")
    int searchCount(NodeCond searchCondition) throws InvalidSearchConditionException;

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
            List<ResourceNameTO> resourceNames);
}
