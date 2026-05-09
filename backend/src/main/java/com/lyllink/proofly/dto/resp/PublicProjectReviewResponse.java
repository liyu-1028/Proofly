package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicProjectReviewResponse {
    private ProjectResponse project;
    private List<ProjectVersionResponse> versions;
    private Long activeVersionId;
    private List<AnnotationResponse> annotations;
    private ConfirmationRecordResponse confirmation;
}
