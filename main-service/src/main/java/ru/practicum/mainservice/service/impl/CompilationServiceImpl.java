package ru.practicum.mainservice.service.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.mainservice.dto.compilation.*;
import ru.practicum.mainservice.exception.NotFoundException;
import ru.practicum.mainservice.mapper.CompilationMapper;
import ru.practicum.mainservice.model.*;
import ru.practicum.mainservice.pagination.OffsetBasedPageRequest;
import ru.practicum.mainservice.repository.*;
import ru.practicum.mainservice.service.CompilationService;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Validated
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {
    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    @Transactional
    @Override
    public @Valid CompilationDto createCompilation(CompilationCreationDto compilationCreationDto) {
        Set<Long> idEvents = compilationCreationDto.getEvents();
        Set<Event> events = new HashSet<>();

        if (idEvents != null && !idEvents.isEmpty()) {
            events = eventRepository.findAllByIdIn(idEvents);
        }

        Compilation compilation = CompilationMapper.toCompilation(compilationCreationDto, events);

        compilationRepository.save(compilation);

        return CompilationMapper.toCompilationDto(compilation);
    }

    @Transactional
    @Override
    public void deleteCompilation(int compId) {
        boolean exist = compilationRepository.existsById(compId);
        if (!exist) {
            throw new NotFoundException(String.format("Compilation with id=%d was not found", compId));
        }

        compilationRepository.deleteById(compId);
    }

    @Transactional
    @Override
    public @Valid CompilationDto updateCompilation(int compId, UpdateCompilationRequestDto updateCompilationRequestDto) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d was not found", compId)));

        Set<Long> idEvents = updateCompilationRequestDto.getEvents();
        Set<Event> events = new HashSet<>();

        if (idEvents != null && !idEvents.isEmpty()) {
            events = eventRepository.findAllByIdIn(idEvents);
        }

        CompilationMapper.fromUpdateDtoToCompilation(updateCompilationRequestDto, compilation, events);

        return CompilationMapper.toCompilationDto(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = new OffsetBasedPageRequest(from, size, Sort.by(Sort.Direction.DESC, "id"));
        List<Compilation> compilations;

        if (pinned != null) {
            BooleanExpression byPinned = QCompilation.compilation.pinned.eq(pinned);
            compilations = compilationRepository.findAll(byPinned, pageable).getContent();
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return compilations.stream().map(CompilationMapper::toCompilationDto).collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(int compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException(String.format("Compilation with id=%d was not found", compId)));

        return CompilationMapper.toCompilationDto(compilation);
    }
}
