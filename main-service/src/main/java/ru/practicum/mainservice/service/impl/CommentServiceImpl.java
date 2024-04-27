package ru.practicum.mainservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mainservice.dto.comment.*;
import ru.practicum.mainservice.exception.*;
import ru.practicum.mainservice.mapper.CommentMapper;
import ru.practicum.mainservice.model.*;
import ru.practicum.mainservice.pagination.OffsetBasedPageRequest;
import ru.practicum.mainservice.repository.*;
import ru.practicum.mainservice.service.CommentService;
import ru.practicum.mainservice.valid.Validator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specification.*;
import static ru.practicum.mainservice.repository.CommentRepository.*;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final Validator validator;

    @Override
    public CommentDto addUserComment(long userId, long eventId, CommentCreationDto commentCreationDto) {
        User commenter = validator.findUserOrThrow(userId);
        Event event = validator.findEventOrThrow(eventId);

        if (!event.getState()
                .equals(EventState.PUBLISHED)) {
            throw new ConflictException("Users are not allowed to comment on unpublished events.");
        }

        Comment comment = CommentMapper.commentCreationDtoToComment(commentCreationDto);
        comment.setUser(commenter);
        comment.setEvent(event);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        comment = commentRepository.save(comment);

        return CommentMapper.commentToCommentDto(comment);
    }

    @Override
    public CommentDto updateUserComment(long userId, long eventId, long commentId,
                                        CommentCreationDto commentCreationDto) {
        Comment comment = validator.findCommentOrThrow(commentId, userId, eventId);

        if (comment.getStatus()
                .equals(CommentStatus.PENDING)) {
            throw new ConflictException("Users are not allowed to update comments, which are pending moderation.");
        }

        comment.setText(commentCreationDto.getText());
        comment.setCreatedOn(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        comment = commentRepository.save(comment);

        return CommentMapper.commentToCommentDto(comment);
    }

    @Override
    public CommentDto getUserEventComment(long userId, long eventId, long commentId) {
        Comment comment = validator.findCommentOrThrow(commentId, userId, eventId);

        if (comment.getStatus()
                .equals(CommentStatus.PENDING)) {
            throw new ConflictException("Users are not allowed to review comments, which are pending moderation.");
        }

        return CommentMapper.commentToCommentDto(comment);
    }

    @Override
    public List<CommentDto> getAllUserComments(long userId) {
        List<Comment> comments = commentRepository.findAllByUserIdAndStatus(userId, CommentStatus.PUBLISHED);

        if (!comments.isEmpty()) {
            return comments.stream()
                    .map(CommentMapper::commentToCommentDto)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public List<CommentDto> getAllUserEventComments(long userId, long eventId) {
        List<Comment> comments = commentRepository.findAllByUserIdAndEventIdAndStatus(userId, eventId, CommentStatus.PUBLISHED);

        if (!comments.isEmpty()) {
            return comments.stream()
                    .map(CommentMapper::commentToCommentDto)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    @Transactional
    public void deleteUserComment(long userId, long eventId, long commentId) {
        if (commentRepository.existsByIdAndUserIdAndEventIdAndStatus(commentId, userId, eventId, CommentStatus.PUBLISHED)) {
            commentRepository.deleteByIdAndUserIdAndEventIdAndStatus(commentId, userId, eventId, CommentStatus.PUBLISHED);
        } else {
            throw new NotFoundException("Comment id = " + commentId + " by user id = " + userId +
                    " for event id = " + eventId + " is pending or doesn't exist");
        }
    }

    @Override
    public List<CommentDto> getAdminComments(String text, List<Long> users, List<CommentStatus> statuses,
                                             List<Long> events,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                             Integer from, Integer size) {

        if ((rangeStart != null && rangeEnd != null) && (rangeStart.isAfter(rangeEnd) || rangeStart.isEqual(rangeEnd))) {
            throw new BadRequestException("Start time must be before the end time!");
        }

        Pageable pageable = new OffsetBasedPageRequest(from, size, Sort.by("id")
                .ascending());

        Page<Comment> commentsPage = commentRepository.findAll(where(hasText(text))
                .and(hasUser(users))
                .and(hasStatus(statuses))
                .and(hasEvent(events))
                .and(isAfter(rangeStart))
                .and(isBefore(rangeEnd)), pageable);

        return commentsPage.stream()
                .map(CommentMapper::commentToCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> moderateAdminComments(CommentStatusUpdateRequestDto updateRequest) {
        List<Comment> comments = commentRepository.findAllByIdInAndStatus(updateRequest.getCommentIds(), CommentStatus.PENDING);

        if (comments.size() != updateRequest.getCommentIds()
                .size()) {
            throw new NotFoundException("Incorrect comment id(s) in the request body.");
        }

        switch (updateRequest.getStatus()) {
            case PUBLISHED:
                comments.forEach(comment -> comment.setStatus(CommentStatus.PUBLISHED));
                comments = commentRepository.saveAll(comments);
                return comments.stream()
                        .map(CommentMapper::commentToCommentDto)
                        .collect(Collectors.toList());
            case DELETED:
                comments.forEach(comment -> commentRepository.deleteAllById(updateRequest.getCommentIds()));
                return new ArrayList<>();
            default:
                throw new BadRequestException("Incorrect admin moderate request with status 'Pending'.");
        }
    }
}
