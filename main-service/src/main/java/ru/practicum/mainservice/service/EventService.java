package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.event.*;
import ru.practicum.mainservice.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto updateEventByEventId(long eventId, UpdateEventRequestDto updateEventRequestDto);

    List<EventFullDto> getEventsAdminApi(List<Long> users, List<EventState> states, List<Integer> categories,
                                         LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto addEvent(long userId, EventCreationDto eventCreationDto);

    List<EventShortDto> getAllUserEvents(long userId, int from, int size);

    EventFullDto getEventByUserAndEventId(long userId, long eventId);

    EventFullDto updateEventByUserIdAndEventId(long userId, long eventId, UpdateEventRequestDto updateEventRequestDto);

    List<EventShortDto> getEventsPublicApi(String text, List<Integer> categories, Boolean paid,
                                           LocalDateTime rangeStart,
                                           LocalDateTime rangeEnd, boolean onlyAvailable, String sort, int from,
                                           int size);

    EventFullDto getEventById(long eventId);
}