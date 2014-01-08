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
package org.apache.syncope.core.quartz;

import org.apache.syncope.core.init.JobInstanceLoader;
import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringBeanJobFactory extends org.springframework.scheduling.quartz.SpringBeanJobFactory {

    private String[] ignoredUnknownProperties;

    private SchedulerContext schedulerContext;

    @Override
    public void setIgnoredUnknownProperties(final String[] ignoredUnknownProperties) {
        String[] defensiveCopy = ignoredUnknownProperties.clone();
        super.setIgnoredUnknownProperties(defensiveCopy);
        this.ignoredUnknownProperties = defensiveCopy;
    }

    @Override
    public void setSchedulerContext(final SchedulerContext schedulerContext) {
        super.setSchedulerContext(schedulerContext);
        this.schedulerContext = schedulerContext;
    }

    /**
     * An implementation of SpringBeanJobFactory that retrieves the bean from the Spring context so that autowiring and
     * transactions work.
     *
     * {@inheritDoc}
     */
    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        final ApplicationContext ctx = ((ConfigurableApplicationContext) schedulerContext.get("applicationContext"));

        // Try to re-create job bean from underlying task (useful for managing failover scenarios)
        if (!ctx.containsBean(bundle.getJobDetail().getKey().getName())) {
            Long taskId = JobInstanceLoader.getTaskIdFromJobName(bundle.getJobDetail().getKey().getName());
            if (taskId != null) {
                JobInstanceLoader jobInstanceLoader = ctx.getBean(JobInstanceLoader.class);
                jobInstanceLoader.registerTaskJob(taskId);
            }

            Long reportId = JobInstanceLoader.getReportIdFromJobName(bundle.getJobDetail().getKey().getName());
            if (reportId != null) {
                JobInstanceLoader jobInstanceLoader = ctx.getBean(JobInstanceLoader.class);
                jobInstanceLoader.registerReportJob(reportId);
            }
        }

        final Object job = ctx.getBean(bundle.getJobDetail().getKey().getName());
        final BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(job);
        if (isEligibleForPropertyPopulation(wrapper.getWrappedInstance())) {
            final MutablePropertyValues pvs = new MutablePropertyValues();
            if (this.schedulerContext != null) {
                pvs.addPropertyValues(this.schedulerContext);
            }
            pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
            pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
            if (this.ignoredUnknownProperties == null) {
                wrapper.setPropertyValues(pvs, true);
            } else {
                for (String propName : this.ignoredUnknownProperties) {
                    if (pvs.contains(propName) && !wrapper.isWritableProperty(propName)) {
                        pvs.removePropertyValue(propName);
                    }
                }
                wrapper.setPropertyValues(pvs);
            }
        }
        return job;
    }
}
