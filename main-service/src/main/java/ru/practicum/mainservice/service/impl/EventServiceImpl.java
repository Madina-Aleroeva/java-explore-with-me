package ru.practicum.mainservice.service.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.client.StatClient;
import ru.practicum.dto.StatResponseDto;
import ru.practicum.mainservice.dto.event.*;
import ru.practicum.mainservice.dto.request.RequestCountDto;
import ru.practicum.mainservice.exception.*;
import ru.practicum.mainservice.mapper.*;
import ru.practicum.mainservice.model.*;
import ru.practicum.mainservice.pagination.OffsetBasedPageRequest;
import ru.practicum.mainservice.repository.*;
import ru.practicum.mainservice.service.EventService;
import ru.practicum.mainservice.valid.Validator;

import javax.validation.Valid;
import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final Validator validator;
    private final StatClient statClient;
    private final RequestRepository requestRepository;

    @Transactional
    @Override
    public @Valid EventFullDto updateEventByEventId(long eventId, UpdateEventRequestDto updateEventRequestDto) {
        LocalDateTime eventDate = null;

        if (updateEventRequestDto.getEventDate() != null) {
            eventDate = updateEventRequestDto.getEventDate();
            Validator.checkEvent1HrAhead(eventDate);
        }

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(String.format("Event with id=%d was not found", eventId)));

        StateAction state = null;
        String stateAction = updateEventRequestDto.getStateAction();

        if (stateAction != null) {
            if (!StateAction.PUBLISH_EVENT.toString().equals(stateAction) && !StateAction.REJECT_EVENT.toString().equals(stateAction)) {
                throw new ConflictException("Field StateAction is incorrect");
            }
            state = StateAction.valueOf(stateAction);

            if (!event.getState().equals(EventState.PENDING) && state.equals(StateAction.PUBLISH_EVENT)) {
                throw new ConflictException("Event must be PENDING state to be published");
            }
            if (event.getState().equals(EventState.PUBLISHED) && state.equals(StateAction.REJECT_EVENT)) {
                throw new ConflictException("Event cannot be canceled if already published");
            }
        }

        Category category = null;
        if (updateEventRequestDto.getCategory() != null) {
            int idCat = updateEventRequestDto.getCategory();
            category = categoryRepository.findById(idCat).orElseThrow(() -> new NotFoundException(String.format("Category with id=%d was not found", idCat)));
        }

        EventMapper.fromUpdateDtoToEvent(updateEventRequestDto, event, category, eventDate, state);

        addViewsAndConfirmedRequestsForEvents(List.of(event));

        return EventMapper.toEventFullDto(event);
    }

    @Override
    public List<EventFullDto> getEventsAdminApi(List<Long> users, List<EventState> states, List<Integer> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        List<Event> events;
        Pageable pageable = new OffsetBasedPageRequest(from, size, Sort.by(Sort.Direction.DESC, "id"));
        BooleanBuilder where = new BooleanBuilder();

        if (!users.isEmpty()) {
            BooleanExpression byUsersId = QEvent.event.initiator.id.in(users);
            where.and(byUsersId);
        }
        if (!states.isEmpty()) {
            BooleanExpression byStates = QEvent.event.state.in(states);
            where.and(byStates);
        }
        if (!categories.isEmpty()) {
            BooleanExpression byCategory = QEvent.event.category.id.in(categories);
            where.and(byCategory);
        }

        BooleanExpression byEventDate;
        if (rangeStart == null && rangeEnd == null) {
            events = eventRepository.findAll(where, pageable).getContent();
            return EventMapper.toListOfEventFullDto(events);
        } else if (rangeStart == null) {
            byEventDate = QEvent.event.eventDate.before(rangeEnd);
        } else if (rangeEnd == null) {
            byEventDate = QEvent.event.eventDate.after(rangeStart);
        } else {
            byEventDate = QEvent.event.eventDate.between(rangeStart, rangeEnd);
        }

        where.and(byEventDate);
        events = eventRepository.findAll(where, pageable).getContent();

        addViewsAndConfirmedRequestsForEvents(events);

        return EventMapper.toListOfEventFullDto(events);
    }

    @Transactional
    @Override
    public @Valid EventFullDto addEvent(long userId, EventCreationDto eventCreationDto) {
        LocalDateTime eventDate = eventCreationDto.getEventDate();
        Validator.checkEvent2HrsAhead(eventDate);

        User user = validator.findUserOrThrow(userId);

        int categoryId = eventCreationDto.getCategory();
        Category category = validator.findCatOrThrow(categoryId);

        Event event = EventMapper.toEvent(eventCreationDto);
        event.setEventDate(eventDate);
        event.setInitiator(user);
        event.setCategory(category);
        event.setState(EventState.PENDING);

        eventRepository.save(event);

        return EventMapper.toEventFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllUserEvents(long userId, int from, int size) {
        Pageable pageable = new OffsetBasedPageRequest(from, size, Sort.by(Sort.Direction.DESC, "id"));

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();

        addViewsAndConfirmedRequestsForEvents(events);

        return EventMapper.toListOfEventShortDto(events);
    }

    @Override
    public EventFullDto getEventByUserAndEventId(long userId, long eventId) {
        Event event = validator.findUserEventOrThrow(eventId, userId);

        addViewsAndConfirmedRequestsForEvents(List.of(event));

        return EventMapper.toEventFullDto(event);
    }

    @Transactional
    @Override
    public @Valid EventFullDto updateEventByUserIdAndEventId(long userId, long eventId, UpdateEventRequestDto updateEventRequestDto) {
        LocalDateTime eventDate = null;
        if (updateEventRequestDto.getEventDate() != null) {
            eventDate = updateEventRequestDto.getEventDate();
            Validator.checkEvent2HrsAhead(eventDate);
        }

        String stateAction = updateEventRequestDto.getStateAction();
        if (stateAction != null) {
            if (!StateAction.SEND_TO_REVIEW.toString().equals(stateAction) && !StateAction.CANCEL_REVIEW.toString().equals(stateAction)) {
                throw new ConflictException("Field StateAction is incorrect");
            }
        }

        StateAction state = null;
        if (stateAction != null) {
            state = StateAction.valueOf(stateAction);
        }

        Category category = null;
        if (updateEventRequestDto.getCategory() != null) {
            category = validator.findCatOrThrow(updateEventRequestDto.getCategory());
        }

        Event event = validator.findUserEventOrThrow(eventId, userId);
        if (!event.getInitiator().getId().equals(userId)) throw new ConflictException("user not found");
        if (event.getState().equals(EventState.PUBLISHED)) throw new ConflictException("already published");


        EventMapper.fromUpdateDtoToEvent(updateEventRequestDto, event, category, eventDate, state);

        addViewsAndConfirmedRequestsForEvents(List.of(event));

        return EventMapper.toEventFullDto(event);
    }

    @Transient
    @Override
    public List<EventShortDto> getEventsPublicApi(String text, List<Integer> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable, String sort, int from, int size) {
        List<Event> events;
        Sort sortOption = Sort.by(Sort.Direction.DESC, "id");

        if (sort.equals(EventSortOption.EVENT_DATE.toString())) {
            sortOption = Sort.by(Sort.Direction.ASC, "eventDate");
        } else if (sort.equals(EventSortOption.VIEWS.toString())) {
            sortOption = Sort.by(Sort.Direction.DESC, "views");
        }

        Pageable pageable = new OffsetBasedPageRequest(from, size, sortOption);
        BooleanBuilder where = new BooleanBuilder();

        BooleanExpression byPublishState = QEvent.event.state.eq(EventState.PUBLISHED);
        where.and(byPublishState);

        if (!text.isBlank()) {
            BooleanExpression byText = QEvent.event.annotation.containsIgnoreCase(text).or(QEvent.event.description.containsIgnoreCase(text));
            where.and(byText);
        }

        if (!categories.isEmpty()) {
            BooleanExpression byCategories = QEvent.event.category.id.in(categories);
            where.and(byCategories);
        }

        if (paid != null) {
            BooleanExpression byPaid = QEvent.event.paid.eq(paid);
            where.and(byPaid);
        }

        BooleanExpression byEventDate;
        if (rangeStart == null && rangeEnd == null) {
            LocalDateTime now = LocalDateTime.now();
            byEventDate = QEvent.event.eventDate.after(now);
        } else if (rangeStart == null) {
            byEventDate = QEvent.event.eventDate.before(rangeEnd);
        } else if (rangeEnd == null) {
            byEventDate = QEvent.event.eventDate.after(rangeStart);
        } else {
            byEventDate = QEvent.event.eventDate.between(rangeStart, rangeEnd);
        }

        where.and(byEventDate);

        events = eventRepository.findAll(where, pageable).getContent();
        if (events.isEmpty()) throw new BadRequestException("No events found");

        addViewsAndConfirmedRequestsForEvents(events);

        if (onlyAvailable) {
            events = events.stream().filter(event -> event.getConfirmedRequest() != event.getParticipantLimit()).collect(Collectors.toList());
        }

        return EventMapper.toListOfEventShortDto(events);
    }

    @Override
    @Transient
    public EventFullDto getEventById(long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(String.format("Event with id=%d was not found", eventId)));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Event is not available because it has not been published yet");
        }

        addViewsAndConfirmedRequestsForEvents(List.of(event));

        return EventMapper.toEventFullDto(event);
    }

    private Map<Long, Long> getHits(List<Long> ids) {
        List<String> uris = ids.stream().map(id -> String.format("/events/%d", id)).collect(Collectors.toList());
        List<StatResponseDto> stats;

        stats = statClient.getStats(DateTimeMapper.fromLocalDateTime(LocalDateTime.now().minusYears(10)), DateTimeMapper.fromLocalDateTime(LocalDateTime.now().plusYears(10)), uris, true);

        Map<Long, Long> hits = new HashMap<>();
        for (StatResponseDto stat : stats) {
            Long id = Long.valueOf(stat.getUri().substring(8));
            hits.put(id, stat.getHits());
        }

        return hits;
    }

    private void addViewsAndConfirmedRequestsForEvents(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> hits = getHits(ids);

        for (Event event : events) {
            event.setViews(hits.getOrDefault(event.getId(), 0L));
        }

        Map<Long, Long> confirmedRequests = requestRepository.findAllConfirmedRequestsByEventIds(ids, RequestStatus.CONFIRMED).stream().collect(Collectors.toMap(RequestCountDto::getId, RequestCountDto::getCount));

        confirmedRequests.entrySet().forEach(entry -> {
            System.out.println("TESTING TEST: " + entry);
        });

        for (Event event : events) {
            System.out.println(event.getId() + " " + confirmedRequests.containsKey(event.getId()) + " " + confirmedRequests);

            event.setConfirmedRequest(confirmedRequests.getOrDefault(event.getId(), 0L));
        }
    }
}
