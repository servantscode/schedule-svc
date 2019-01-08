package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(5000)
public class ServiceIdentificationFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(ServiceIdentificationFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {

        ThreadContext.put("service.name", "schedule");

        // Uncomment to enable service entry logging
//        if(!requestContext.getMethod().equalsIgnoreCase("OPTIONS"))
//            LOG.info(String.format("Service %s %s initiated.",
//                    ThreadContext.get("request.method"),
//                    ThreadContext.get("request.path")));
    }
}
