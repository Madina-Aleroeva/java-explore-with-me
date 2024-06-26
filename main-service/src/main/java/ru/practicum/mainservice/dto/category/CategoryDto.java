package ru.practicum.mainservice.dto.category;

import lombok.*;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
public class CategoryDto {
    private int id;
    @Length(min = 2, max = 50)
    private String name;
}
