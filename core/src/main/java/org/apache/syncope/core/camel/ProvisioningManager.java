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

package org.apache.syncope.core.camel;

import org.apache.camel.model.ModelCamelContext;

/**
 *
 * @author giacomolm
 */
public interface ProvisioningManager {
    
    public void startContext() throws Exception;
    
    public void stopContext() throws Exception;
    
    public ModelCamelContext getContext();
    
    public void changeRoute(String routePath);
    
    public void sendMessage(String uri, Object obj);
    
    public void startConsumer(String uri) throws Exception;
    
    public Object getMessage(Class type) throws Exception;
}