package com.lyllink.proofly.dto.resp;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardStatsResponse {
    private Map<String, Long> statusCounts;
    private Long totalProjects;
    private List<ProjectResponse> recentProjects;
    private List<AuditLogResponse> recentActivities;
}
