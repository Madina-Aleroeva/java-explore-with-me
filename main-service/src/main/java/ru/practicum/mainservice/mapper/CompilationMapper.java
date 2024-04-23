package ru.practicum.mainservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.mainservice.dto.compilation.*;
import ru.practicum.mainservice.model.*;

import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public static Compilation toCompilation(CompilationCreationDto compilationCreationDto, Set<Event> events) {
        return Compilation.builder().title(compilationCreationDto.getTitle()).pinned(compilationCreationDto.isPinned()).events(events).build();
    }

    public static CompilationDto toCompilationDto(Compilation compilation) {
        return CompilationDto.builder().id(compilation.getId()).title(compilation.getTitle()).pinned(compilation.isPinned()).events(compilation.getEvents().stream().map(EventMapper::toEventShortDto).collect(Collectors.toList())).build();
    }

    public static Compilation fromUpdateDtoToCompilation(UpdateCompilationRequestDto updateDto, Compilation compilation, Set<Event> events) {
        compilation.setTitle((updateDto.getTitle() == null || updateDto.getTitle().isBlank()) ? compilation.getTitle() : updateDto.getTitle());
        compilation.setPinned(updateDto.getPinned() == null ? compilation.isPinned() : updateDto.getPinned());
        compilation.setEvents(updateDto.getEvents() == null ? compilation.getEvents() : events);

        return compilation;
    }
}
