package ru.practicum.mainservice.dto.request;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
public class RequestStatusUpdateRequestDto {
    @NotNull
    private List<Long> requestIds;
    @NotNull
    private StatusOfUpdateRequest status;
}
