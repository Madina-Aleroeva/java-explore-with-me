package ru.practicum.mainservice.dto.user;

import lombok.*;

import javax.validation.constraints.*;

@Data
@Builder
public class UserShortDto {
    @Positive
    private long id;

    @NotBlank
    private String name;
}
