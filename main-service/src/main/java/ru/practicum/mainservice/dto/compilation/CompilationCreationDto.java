package ru.practicum.mainservice.dto.compilation;

import lombok.*;

import javax.validation.constraints.*;
import java.util.Set;

@Data
@Builder
public class CompilationCreationDto {
    @NotBlank
    @Size(min = 2, max = 50)
    private String title;
    private Set<Long> events;
    private boolean pinned;
}
