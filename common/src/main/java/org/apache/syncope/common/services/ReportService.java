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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.to.ReportExecTO;
import org.apache.syncope.common.to.ReportTO;
import org.apache.syncope.common.types.ReportExecExportFormat;
import org.apache.syncope.common.wrap.ReportletConfClass;

@Path("reports")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface ReportService extends JAXRSService {

    /**
     * @param reportTO Report to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created report
     */
    @POST
    Response create(ReportTO reportTO);

    /**
     * @param reportId Deletes report with matching id
     */
    @DELETE
    @Path("{reportId}")
    void delete(@PathParam("reportId") Long reportId);

    /**
     * @param executionId ID of execution report to be deleted
     */
    @DELETE
    @Path("executions/{executionId}")
    void deleteExecution(@PathParam("executionId") Long executionId);

    /**
     * @param reportId ID of report to be executed.
     * @return Execution result
     */
    @POST
    @Path("{reportId}/execute")
    ReportExecTO execute(@PathParam("reportId") Long reportId);

    /**
     * @param executionId ID of execution report to be selected
     * @param fmt file-format selection
     * @return Returns a stream for content download
     */
    @GET
    @Path("executions/{executionId}/stream")
    Response exportExecutionResult(@PathParam("executionId") Long executionId,
            @QueryParam("format") ReportExecExportFormat fmt);

    /**
     * @return Returns a list of all reportletConfClasses
     */
    @GET
    @Path("reportletConfClasses")
    List<ReportletConfClass> getReportletConfClasses();

    /**
     * @return Paged list of all existing reports
     */
    @GET
    PagedResult<ReportTO> list();

    /**
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of all existing reports
     */
    @GET
    PagedResult<ReportTO> list(@QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * @param page selected page in relation to size
     * @param size number of entries per page
     * @return Paged list of existing reports matching page/size conditions
     */
    @GET
    PagedResult<ReportTO> list(@QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size);

    /**
     * @param page selected page in relation to size
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return Paged list of existing reports matching page/size conditions
     */
    @GET
    PagedResult<ReportTO> list(@QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) int page,
            @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) int size,
            @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * @param reportId ID of report to be read
     * @return Report with matching ID
     */
    @GET
    @Path("{reportId}")
    ReportTO read(@PathParam("reportId") Long reportId);

    /**
     * @param executionId ID ExecutionReport to be selected
     * @return Returns ExecutionReport with matching id
     */
    @GET
    @Path("executions/{executionId}")
    ReportExecTO readExecution(@PathParam("executionId") Long executionId);

    /**
     * @param reportId ID for report to be updated
     * @param reportTO Updates report with matching reportId
     */
    @PUT
    @Path("{reportId}")
    void update(@PathParam("reportId") Long reportId, ReportTO reportTO);
}
