package com.lyllink.proofly.dto.resp;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmationRecordResponse {
    private Long id;
    private Long versionId;
    private String customerName;
    private LocalDateTime confirmedAt;
}
