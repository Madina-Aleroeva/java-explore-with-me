package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.compilation.*;

import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(CompilationCreationDto compilationCreationDto);

    void deleteCompilation(int compId);

    CompilationDto updateCompilation(int compId, UpdateCompilationRequestDto updateCompilationRequestDto);

    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    CompilationDto getCompilationById(int compId);
}
