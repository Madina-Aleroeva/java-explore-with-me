package ru.practicum.mainservice.dto.category;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class CategoryCreationDto {
    @Size(min = 2, max = 50)
    @NotBlank
    private String name;
}
