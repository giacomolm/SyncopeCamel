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
package org.apache.syncope.common;

public class SyncopeConstants {

    public static final String NAMESPACE = "http://syncope.apache.org/1.2";

    public static final String UNAUTHENTICATED = "unauthenticated";

    public static final String ANONYMOUS_ENTITLEMENT = "anonymous";

    public static final String ENUM_VALUES_SEPARATOR = ";";

    public static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ssz",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss.S", // explicitly added to import date into MySql repository
        "yyyy-MM-dd" };

    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String ROOT_LOGGER = "ROOT";

}
