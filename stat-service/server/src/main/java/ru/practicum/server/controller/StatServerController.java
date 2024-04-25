package ru.practicum.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.*;
import ru.practicum.server.service.StatsService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class StatServerController {

    private final StatsService statService;

    @PostMapping("/hit")
    @ResponseStatus(value = HttpStatus.CREATED)
    public void addHit(@RequestBody @Valid EndpointHitRequestDto requestDto) {
        statService.addHit(requestDto);
    }

    @GetMapping("/stats")
    public ResponseEntity<List<StatResponseDto>> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                                         @RequestParam(defaultValue = "") List<String> uris,
                                                         @RequestParam(defaultValue = "false") boolean unique) {
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(statService.getStats(start, end, uris, unique));
    }
}