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
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.mod.ResourceAssociationMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeAssociationActionType;

@Path("users")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface UserService extends JAXRSService {

    @OPTIONS
    @Path("{userId}/username")
    Response getUsername(@PathParam("userId") Long userId);

    @OPTIONS
    @Path("{username}/userId")
    Response getUserId(@PathParam("username") String username);

    /**
     * Reads the user matching the provided userId.
     *
     * @param userId id of user to be read
     * @return User matching the provided userId
     */
    @GET
    @Path("{userId}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Reads the user matching the provided userId"),
        @Description(target = DocTarget.RETURN, value = "User matching the provided userId")
    })
    UserTO read(@Description("id of user to be read") @PathParam("userId") Long userId);

    /**
     * Returns a paged list of existing users.
     *
     * @return Paged list of all existing users
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Returns a list of all existing users"),
        @Description(target = DocTarget.RETURN, value = "Paged list of all existing users")
    })
    PagedResult<UserTO> list();

    /**
     * Returns a paged list of existing users.
     *
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of all existing users
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Returns a list of all existing users"),
        @Description(target = DocTarget.RETURN, value = "Paged list of all existing users")
    })
    PagedResult<UserTO> list(
            @Description("list of ordering clauses, separated by comma") @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of existing users matching page/size conditions.
     *
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of existing users matching page/size conditions
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns a list of all existing users matching page/size conditions"),
        @Description(target = DocTarget.RETURN, value = "Paged list of existing users matching page/size conditions")
    })
    PagedResult<UserTO> list(
            @Description("result page number")
            @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @Description("number of entries per page")
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size,
            @Description("list of ordering clauses, separated by comma") @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of existing users matching page/size conditions.
     *
     * @param page result page number
     * @param size number of entries per page
     * @return Paged list of existing users matching page/size conditions
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns a list of all existing users matching page/size conditions"),
        @Description(target = DocTarget.RETURN, value = "Paged list of existing users matching page/size conditions")
    })
    PagedResult<UserTO> list(
            @Description("result page number")
            @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @Description("number of entries per page")
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @return Paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns a paged list of users matching the provided FIQL search condition"),
        @Description(target = DocTarget.RETURN, value = "List of users matching the given condition")
    })
    PagedResult<UserTO> search(@Description("FIQL search expression") @QueryParam(PARAM_FIQL) String fiql);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns a paged list of users matching the provided FIQL search condition"),
        @Description(target = DocTarget.RETURN, value = "List of users matching the given condition")
    })
    PagedResult<UserTO> search(@Description("FIQL search expression") @QueryParam(PARAM_FIQL) String fiql,
            @Description("list of ordering clauses, separated by comma") @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @return Paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns a paged list of users matching the provided FIQL search condition"),
        @Description(target = DocTarget.RETURN,
                value = "Paged list of users matching the provided FIQL search condition")
    })
    PagedResult<UserTO> search(@Description("FIQL search expression") @QueryParam(PARAM_FIQL) String fiql,
            @Description("result page number")
            @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @Description("number of entries per page")
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns a paged list of users matching the provided FIQL search condition"),
        @Description(target = DocTarget.RETURN,
                value = "Paged list of users matching the provided FIQL search condition")
    })
    PagedResult<UserTO> search(@Description("FIQL search expression") @QueryParam(PARAM_FIQL) String fiql,
            @Description("result page number")
            @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @Description("number of entries per page")
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size,
            @Description("list of ordering clauses, separated by comma") @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Creates a new user.
     *
     * @param userTO user to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created user as well as the user itself
     * enriched with propagation status information, as <tt>Entity</tt>
     */
    @POST
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Creates a new user"),
        @Description(target = DocTarget.RETURN,
                value = "Response object featuring <tt>Location</tt> header of created user"),
        @Description(target = DocTarget.RESPONSE,
                value = "User created available at URL specified via the <tt>Location</tt> header")
    })
    Response create(@Description("user to be created") UserTO userTO);

    /**
     * Updates user matching the provided userId.
     *
     * @param userId id of user to be updated
     * @param userMod modification to be applied to user matching the provided userId
     * @return <tt>Response</tt> object featuring the updated user enriched with propagation status information,
     * as <tt>Entity</tt>
     */
    @POST
    @Path("{userId}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Updates user matching the provided userId"),
        @Description(target = DocTarget.RETURN, value = "<tt>Response</tt> object featuring the updated user enriched "
                + "with propagation status information, as <tt>Entity</tt>")
    })
    Response update(@Description("id of user to be updated") @PathParam("userId") Long userId,
            @Description("modification to be applied to user matching the provided userId") UserMod userMod);

    /**
     * Performs a status update on user matching provided userId.
     *
     * @param userId id of user to be subjected to status update
     * @param statusMod status update details
     * @return <tt>Response</tt> object featuring the updated user enriched with propagation status information,
     * as <tt>Entity</tt>
     */
    @POST
    @Path("{userId}/status")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Performs a status update on user matching provided userId"),
        @Description(target = DocTarget.RETURN, value = "<tt>Response</tt> object featuring the updated user enriched "
                + "with propagation status information, as <tt>Entity</tt>")
    })
    Response status(@Description("id of user to be subjected to status update") @PathParam("userId") Long userId,
            @Description("status update details") StatusMod statusMod);

    /**
     * Deletes user matching provided userId.
     *
     * @param userId id of user to be deleted
     * @return <tt>Response</tt> object featuring the deleted user enriched with propagation status information,
     * as <tt>Entity</tt>
     */
    @DELETE
    @Path("{userId}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Deletes user matching provided userId"),
        @Description(target = DocTarget.RETURN, value = "<tt>Response</tt> object featuring the deleted user enriched "
                + "with propagation status information, as <tt>Entity</tt>")
    })
    Response delete(@Description("id of user to be deleted") @PathParam("userId") Long userId);

    /**
     * Executes resource-related operations on given user.
     *
     * @param userId user id.
     * @param type resource de-association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring the bulk action result
     */
    @POST
    @Path("{userId}/bulkDeassociation/{type}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Executes resource-related operations on given user"),
        @Description(target = DocTarget.RETURN, value = "<tt>Response</tt> object featuring the bulk action result")
    })
    Response bulkDeassociation(@Description("user id") @PathParam("userId") Long userId,
            @Description("resource de-association action type") @PathParam("type") ResourceDeAssociationActionType type,
            @Description("external resources to be used for propagation-related operations") List<ResourceName> resourceNames);

    /**
     * Executes resource-related operations on given user.
     *
     * @param userId user id.
     * @param type resource association action type
     * @param associationMod external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring the bulk action result
     */
    @POST
    @Path("{userId}/bulkAssociation/{type}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Executes resource-related operations on given user"),
        @Description(target = DocTarget.RETURN, value = "<tt>Response</tt> object featuring the bulk action result")
    })
    Response bulkAssociation(@Description("user id") @PathParam("userId") Long userId,
            @Description("resource association action type") @PathParam("type") ResourceAssociationActionType type,
            @Description("external resources to be used for propagation-related operations") ResourceAssociationMod associationMod);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of &lt;username, action&gt; pairs
     * @return Bulk action result.
     */
    @POST
    @Path("bulk")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Executes the provided bulk action"),
        @Description(target = DocTarget.RETURN, value = "Bulk action result")
    })
    BulkActionResult bulk(@Description("list of &lt;username, action&gt; pairs") BulkAction bulkAction);
}
