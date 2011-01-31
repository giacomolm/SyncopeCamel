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
package org.syncope.console;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authentication.AuthenticatedWebSession;
import org.apache.wicket.authorization.strategies.role.RoleAuthorizationStrategy;
import org.apache.wicket.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.pages.Configuration;
import org.syncope.console.pages.Connectors;
import org.syncope.console.pages.Login;
import org.syncope.console.pages.Logout;
import org.syncope.console.pages.Report;
import org.syncope.console.pages.Resources;
import org.syncope.console.pages.Roles;
import org.syncope.console.pages.Schema;
import org.syncope.console.pages.Tasks;
import org.syncope.console.pages.Users;
import org.syncope.console.pages.WelcomePage;

/**
 * SyncopeApplication class.
 */
public class SyncopeApplication extends AuthenticatedWebApplication {

    private SyncopeUser user = null;

    @Override
    protected void init() {
        addComponentInstantiationListener(getSpringInjector());
        getResourceSettings().setThrowExceptionOnMissingResource(true);

        getSecuritySettings().setAuthorizationStrategy(
                new RoleAuthorizationStrategy(new SyncopeRolesAuthorizer()));

        // setup authorizations
        MetaDataRoleAuthorizationStrategy.authorize(Schema.class,
                "SCHEMA_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Roles.class,
                "ROLE_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Connectors.class,
                "CONNECTOR_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Resources.class,
                "RESOURCE_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Users.class,
                "USER_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Report.class,
                "REPORT_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Tasks.class,
                "TASK_LIST");
        MetaDataRoleAuthorizationStrategy.authorize(Configuration.class,
                "CONFIGURATION_LIST");
    }

    public void setupNavigationPane(final WebPage page,
            final XMLRolesReader xmlRolesReader) {

        BookmarkablePageLink schemaLink =
                new BookmarkablePageLink("schema", Schema.class);
        String allowedSchemaRoles =
                xmlRolesReader.getAllAllowedRoles("Schema", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                schemaLink, WebPage.ENABLE, allowedSchemaRoles);
        page.add(schemaLink);

        BookmarkablePageLink usersLink =
                new BookmarkablePageLink("users", Users.class);
        String allowedUsersRoles =
                xmlRolesReader.getAllAllowedRoles("Users", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                usersLink, WebPage.ENABLE, allowedUsersRoles);
        page.add(usersLink);

        BookmarkablePageLink rolesLink =
                new BookmarkablePageLink("roles", Roles.class);
        String allowedRoleRoles =
                xmlRolesReader.getAllAllowedRoles("Roles", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                rolesLink, WebPage.ENABLE, allowedRoleRoles);
        page.add(rolesLink);

        BookmarkablePageLink resourcesLink =
                new BookmarkablePageLink("resources", Resources.class);
        String allowedResourcesRoles =
                xmlRolesReader.getAllAllowedRoles("Resources", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                resourcesLink, WebPage.ENABLE, allowedResourcesRoles);
        page.add(resourcesLink);

        BookmarkablePageLink connectorsLink =
                new BookmarkablePageLink("connectors", Connectors.class);
        String allowedConnectorsRoles =
                xmlRolesReader.getAllAllowedRoles("Connectors", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                connectorsLink, WebPage.ENABLE, allowedConnectorsRoles);
        page.add(connectorsLink);

        BookmarkablePageLink reportLink =
                new BookmarkablePageLink("report", Report.class);
        String allowedReportRoles =
                xmlRolesReader.getAllAllowedRoles("Report", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                reportLink, WebPage.ENABLE, allowedReportRoles);
        page.add(reportLink);

        BookmarkablePageLink configurationLink =
                new BookmarkablePageLink("configuration", Configuration.class);
        String allowedConfigurationRoles =
                xmlRolesReader.getAllAllowedRoles("Configuration", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                configurationLink, WebPage.ENABLE, allowedConfigurationRoles);
        page.add(configurationLink);

        BookmarkablePageLink taskLink =
                new BookmarkablePageLink("tasks", Tasks.class);
        String allowedTasksRoles =
                xmlRolesReader.getAllAllowedRoles("Tasks", "list");
        MetaDataRoleAuthorizationStrategy.authorize(
                taskLink, WebPage.ENABLE, allowedTasksRoles);
        page.add(taskLink);

        page.add(new BookmarkablePageLink("logout", Logout.class));
    }

    /**
     * Create a new custom SyncopeSession
     * @param request
     * @param response
     * @return Session
     */
    @Override
    public Session newSession(final Request request, final Response response) {
        SyncopeSession session = new SyncopeSession(request);

        if (user != null) {
            session.setUser(user);
        }

        return session;
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class getHomePage() {
        return (((SyncopeSession) Session.get()).getUser() == null)
                ? Login.class : WelcomePage.class;
    }

    @Override
    public final RequestCycle newRequestCycle(final Request request,
            final Response response) {

        return new SyncopeRequestCycle(this, (WebRequest) request,
                (WebResponse) response);
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass() {
        return SyncopeSession.class;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return Login.class;
    }

    protected SpringComponentInjector getSpringInjector(){

        return new SpringComponentInjector(this);
    }
}
