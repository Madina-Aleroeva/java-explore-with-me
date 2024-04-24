package ru.practicum.mainservice.service;

import ru.practicum.mainservice.dto.request.*;

import java.util.List;

public interface RequestService {
    RequestDto createRequest(long userId, long eventId);

    List<RequestDto> getUserRequests(long userId);

    RequestDto cancelRequest(long userId, long requestId);

    List<RequestDto> getRequestsByEventId(long userId, long eventId);

    RequestStatusUpdateResultDto updateRequestsStatus(long userId, long eventId,
                                                      RequestStatusUpdateRequestDto updateDto);
}
