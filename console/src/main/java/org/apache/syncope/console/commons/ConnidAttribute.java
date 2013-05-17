/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.console.commons;

/**
 *
 * @author massi
 */
public final class ConnidAttribute {
    
    public static final String ENABLE = "__ENABLE__";

    public static final String NAME = "__NAME__";

    public static final String UID = "__UID__";

    public static final String PASSWORD = "__PASSWORD__";
    
    private ConnidAttribute() {
        // private constructor for static utility class
    }
    
}