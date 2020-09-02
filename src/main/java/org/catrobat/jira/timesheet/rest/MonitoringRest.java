/*
 * Copyright 2016 Adrian Schnedlitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catrobat.jira.timesheet.rest;


import org.catrobat.jira.timesheet.activeobjects.Monitoring;
import org.catrobat.jira.timesheet.rest.json.JsonMonitoring;
import org.catrobat.jira.timesheet.services.MonitoringService;
import org.catrobat.jira.timesheet.services.PermissionService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Path("/monitoring")
@Produces({MediaType.APPLICATION_JSON})
public class MonitoringRest {
    private final PermissionService permissionService;
    private final MonitoringService monitoringService;
    private final org.apache.log4j.Logger LOGGER = org.apache.log4j.LogManager.getLogger(MonitoringRest.class);

    public MonitoringRest(final PermissionService permissionService, MonitoringService monitoringService) {
        this.permissionService = permissionService;
        this.monitoringService = monitoringService;
    }

    @GET
    @Path("/getMonitoring")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitoring(@Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        Monitoring monitoring = monitoringService.getMonitoring();
        JsonMonitoring jsonMonitoring = new JsonMonitoring(monitoring);
        jsonMonitoring.setPeriodTime(monitoringService.getLastIntervalFormattedAsString());

        return Response.ok(jsonMonitoring).build();
    }

    @PUT
    @Path("/saveMonitoring")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setMonitoring(final JsonMonitoring jsonMonitoring, @Context HttpServletRequest request) {
        Response unauthorized = permissionService.checkRootPermission();
        if (unauthorized != null) {
            return unauthorized;
        }

        monitoringService.setMonitoring(jsonMonitoring.getPeriod(),jsonMonitoring.getRequiredHours(),
                jsonMonitoring.getExceptions());

        return Response.noContent().build();
    }
}
