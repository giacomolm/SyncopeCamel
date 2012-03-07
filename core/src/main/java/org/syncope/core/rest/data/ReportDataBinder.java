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
package org.syncope.core.rest.data;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.syncope.client.report.ReportletConf;
import org.syncope.client.to.ReportExecTO;
import org.syncope.client.to.ReportTO;
import org.syncope.core.init.JobInstanceLoader;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.beans.ReportExec;
import org.syncope.core.persistence.dao.ReportExecDAO;
import org.syncope.core.report.Reportlet;
import org.syncope.core.report.ReportletConfClass;

@Component
public class ReportDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ReportDataBinder.class);

    private static final String[] IGNORE_REPORT_PROPERTIES = {
        "id", "reportlets", "executions", "latestExecStatus"};

    private static final String[] IGNORE_REPORT_EXECUTION_PROPERTIES = {
        "id", "report", "execResult"};

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private ResourcePatternResolver resResolver;

    public Set<Class<Reportlet>> getAllReportletClasses() {
        CachingMetadataReaderFactory cachingMetadataReaderFactory = new CachingMetadataReaderFactory();

        Set<Class<Reportlet>> reportletClasses = new HashSet<Class<Reportlet>>();
        try {
            for (Resource resource : resResolver.getResources("classpath*:**/*.class")) {
                ClassMetadata metadata = cachingMetadataReaderFactory.getMetadataReader(resource).getClassMetadata();

                try {
                    Class reportletClass =
                            ClassUtils.forName(metadata.getClassName(), ClassUtils.getDefaultClassLoader());
                    Set<Class> interfaces = ClassUtils.getAllInterfacesForClassAsSet(reportletClass);
                    if (interfaces.contains(Reportlet.class) && !metadata.isAbstract()) {
                        reportletClasses.add(reportletClass);
                    }
                } catch (ClassNotFoundException e) {
                    LOG.error("Could not load class {}", metadata.getClassName(), e);
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}", Reportlet.class.getName(), e);
        }

        return reportletClasses;
    }

    public Class<? extends ReportletConf> getReportletConfClass(final Class<Reportlet> reportletClass) {
        Class<? extends ReportletConf> result = null;

        ReportletConfClass annotation = reportletClass.getAnnotation(ReportletConfClass.class);
        if (annotation != null) {
            result = annotation.value();
        }

        return result;
    }

    public Class<Reportlet> findReportletClassHavingConfClass(final Class<? extends ReportletConf> reportletConfClass) {
        Class<Reportlet> result = null;
        for (Class<Reportlet> reportletClass : getAllReportletClasses()) {
            Class<? extends ReportletConf> found = getReportletConfClass(reportletClass);
            if (found != null && found.equals(reportletConfClass)) {
                result = reportletClass;
            }
        }

        return result;
    }

    public void getReport(final Report report, final ReportTO reportTO) {
        BeanUtils.copyProperties(reportTO, report, IGNORE_REPORT_PROPERTIES);
        report.setReportletConfs(null);
        for (ReportletConf conf : reportTO.getReportletConfs()) {
            report.addReportletConf(conf);
        }
    }

    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setId(report.getId());
        BeanUtils.copyProperties(report, reportTO, IGNORE_REPORT_PROPERTIES);

        reportTO.setReportletConfs(report.getReportletConfs());

        ReportExec latestExec = reportExecDAO.findLatestStarted(report);
        reportTO.setLatestExecStatus(latestExec == null
                ? "" : latestExec.getStatus());

        for (ReportExec reportExec : report.getExecs()) {
            reportTO.addExecution(getReportExecTO(reportExec));
        }

        String triggerName = JobInstanceLoader.getTriggerName(
                JobInstanceLoader.getJobName(report));

        Trigger trigger;
        try {
            trigger = scheduler.getScheduler().getTrigger(triggerName,
                    Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
            trigger = null;
        }

        if (trigger != null) {
            reportTO.setLastExec(trigger.getPreviousFireTime());
            reportTO.setNextExec(trigger.getNextFireTime());
        }

        return reportTO;
    }

    public ReportExecTO getReportExecTO(final ReportExec execution) {
        ReportExecTO executionTO = new ReportExecTO();
        executionTO.setId(execution.getId());
        BeanUtils.copyProperties(execution, executionTO,
                IGNORE_REPORT_EXECUTION_PROPERTIES);
        if (execution.getId() != null) {
            executionTO.setId(execution.getId());
        }
        executionTO.setReport(execution.getReport().getId());

        return executionTO;
    }
}
