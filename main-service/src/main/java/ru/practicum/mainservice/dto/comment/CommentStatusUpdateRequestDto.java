package ru.practicum.mainservice.dto.comment;


import lombok.*;
import ru.practicum.mainservice.model.CommentStatus;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentStatusUpdateRequestDto {
    @NotNull
    @NotEmpty
    private List<Long> commentIds;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CommentStatus status;
}
