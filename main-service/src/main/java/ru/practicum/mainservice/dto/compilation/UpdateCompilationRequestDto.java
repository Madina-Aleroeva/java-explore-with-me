package ru.practicum.mainservice.dto.compilation;

import lombok.*;

import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Builder
public class UpdateCompilationRequestDto {
    @Size(min = 2, max = 50)
    private String title;
    private Set<Long> events;
    private Boolean pinned;
}
