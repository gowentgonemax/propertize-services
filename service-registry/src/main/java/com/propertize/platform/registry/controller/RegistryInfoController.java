package com.propertize.platform.registry.controller;

import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller to provide human-readable registry information
 */
@RestController
@RequestMapping("/registry")
public class RegistryInfoController {

    @GetMapping("/apps")
    public Map<String, Object> getRegisteredApps() {
        Map<String, Object> response = new HashMap<>();

        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            PeerAwareInstanceRegistry registry = serverContext.getRegistry();
            Applications applications = registry.getApplications();

            List<Map<String, Object>> appsList = applications.getRegisteredApplications().stream()
                    .map(this::mapApplication)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("totalApplications", appsList.size());
            response.put("totalInstances", applications.size());
            response.put("applications", appsList);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    private Map<String, Object> mapApplication(Application app) {
        Map<String, Object> appMap = new HashMap<>();
        appMap.put("name", app.getName());
        appMap.put("instanceCount", app.getInstances().size());

        List<Map<String, Object>> instances = app.getInstances().stream()
                .map(instance -> {
                    Map<String, Object> inst = new HashMap<>();
                    inst.put("instanceId", instance.getInstanceId());
                    inst.put("hostName", instance.getHostName());
                    inst.put("ipAddress", instance.getIPAddr());
                    inst.put("port", instance.getPort());
                    inst.put("status", instance.getStatus().name());
                    inst.put("healthCheckUrl", instance.getHealthCheckUrl());
                    inst.put("homePageUrl", instance.getHomePageUrl());
                    inst.put("statusPageUrl", instance.getStatusPageUrl());
                    inst.put("lastUpdated", instance.getLastUpdatedTimestamp());
                    return inst;
                })
                .collect(Collectors.toList());

        appMap.put("instances", instances);
        return appMap;
    }

    @GetMapping("/health")
    public Map<String, Object> getRegistryHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            PeerAwareInstanceRegistry registry = serverContext.getRegistry();
            Applications applications = registry.getApplications();

            long upCount = applications.getRegisteredApplications().stream()
                    .flatMap(app -> app.getInstances().stream())
                    .filter(instance -> "UP".equals(instance.getStatus().name()))
                    .count();

            long downCount = applications.getRegisteredApplications().stream()
                    .flatMap(app -> app.getInstances().stream())
                    .filter(instance -> !"UP".equals(instance.getStatus().name()))
                    .count();

            health.put("totalApplications", applications.getRegisteredApplications().size());
            health.put("totalInstances", applications.size());
            health.put("instancesUp", upCount);
            health.put("instancesDown", downCount);
            health.put("status", downCount == 0 ? "HEALTHY" : "DEGRADED");

        } catch (Exception e) {
            health.put("status", "ERROR");
            health.put("error", e.getMessage());
        }

        return health;
    }
}
