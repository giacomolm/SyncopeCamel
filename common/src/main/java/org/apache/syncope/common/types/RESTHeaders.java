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
package org.apache.syncope.common.types;

/**
 * Custom HTTP headers in use with REST services.
 */
public final class RESTHeaders {

    /**
     * UserId option key.
     */
    public static final String USER_ID = "Syncope.UserId";

    /**
     * Username option key.
     */
    public static final String USERNAME = "Syncope.Username";

    /**
     * Option key stating if user request create is allowed or not.
     */
    public static final String SELFREGISTRATION_ALLOWED = "Syncope.SelfRegistration.Allowed";

    /**
     * Option key stating if Activiti workflow adapter is in use for users.
     */
    public static final String ACTIVITI_USER_ENABLED = "Syncope.Activiti.User.Enabled";

    /**
     * Option key stating if Activiti workflow adapter is in use for roles.
     */
    public static final String ACTIVITI_ROLE_ENABLED = "Syncope.Activiti.Role.Enabled";

    /**
     * HTTP header key for object ID assigned to an object after its creation.
     */
    public static final String RESOURCE_ID = "Syncope.Id";

    /**
     * Declares the type of exception being raised.
     */
    public static final String EXCEPTION_TYPE = "Syncope.ExceptionType";

    /**
     * Mediatype for PNG images, not defined in <tt>javax.ws.rs.core.MediaType</tt>.
     *
     * @see javax.ws.rs.core.MediaType
     */
    public static final String MEDIATYPE_IMAGE_PNG = "image/png";

    /**
     * Allows the client to specify a preference for the result to be returned from the server.
     * <a href="http://msdn.microsoft.com/en-us/library/hh537533.aspx">More information</a>.
     *
     * @see Preference
     */
    public static final String PREFER = "Prefer";

    /**
     * Allowd the server to inform the client about the fact that a specified preference was applied.
     * <a href="http://msdn.microsoft.com/en-us/library/hh554623.aspx">More information</a>.
     *
     * @see Preference
     */
    public static final String PREFERENCE_APPLIED = "Preference-Applied";

    private RESTHeaders() {
        // Empty constructor for static utility class.
    }
}
