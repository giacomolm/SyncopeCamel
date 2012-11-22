/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.core.workflow.UserWorkflowAdapter;
import org.syncope.core.workflow.WorkflowException;

@Controller
@RequestMapping("/workflow")
public class WorkflowController extends AbstractController {

    @Autowired
    private UserWorkflowAdapter wfAdapter;

    @PreAuthorize("hasRole('WORKFLOW_DEF_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/definition")
    @Transactional(readOnly = true)
    public WorkflowDefinitionTO getDefinition()
            throws WorkflowException {

        return wfAdapter.getDefinition();
    }

    @PreAuthorize("hasRole('WORKFLOW_DEF_UPDATE')")
    @RequestMapping(method = RequestMethod.PUT,
    value = "/definition")
    public void updateDefinition(
            @RequestBody final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        wfAdapter.updateDefinition(definition);
    }

    @PreAuthorize("hasRole('WORKFLOW_TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/tasks")
    public ModelAndView getDefinedTasks()
            throws WorkflowException {

        return new ModelAndView().addObject(wfAdapter.getDefinedTasks());
    }
}