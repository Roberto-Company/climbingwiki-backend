package com.roberto.climbingwiki.exceptions;

import com.roberto.climbingwiki.security.InvalidTokenException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({RouteNotFoundException.class, SectorNotFoundException.class,
            UserNotFoundException.class, TagNotFoundException.class, ZoneNotFoundException.class,
            CragNotFoundException.class, PitchNotFoundException.class, GradeNotFoundException.class,
            RatingNotFoundException.class, GradingSystemNotFoundException.class, PhotoNotFoundException.class,
            CommentNotFoundException.class, LogbookEntryNotFoundException.class,
            FavoriteNotFoundException.class, FollowNotFoundException.class, RouteListNotFoundException.class,
            ListRouteNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({PitchOrderConflictException.class, UserConflictException.class,
            GradingSystemConflictException.class, GradeConflictException.class,
            CatalogItemInUseException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({InvalidRatingScoreException.class, InvalidPhotoTargetException.class,
            InvalidZoneParentException.class, InvalidCommentTargetException.class,
            InvalidCommentParentException.class, InvalidFollowException.class,
            InvalidOrExpiredTokenException.class})
    public ResponseEntity<ApiError> handleBusinessRuleViolation(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}
