package ru.practicum.server.service;

import ru.practicum.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void addHit(EndpointHitRequestDto requestDto);

    List<StatResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
