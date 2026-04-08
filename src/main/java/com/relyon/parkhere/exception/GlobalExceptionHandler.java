package com.relyon.parkhere.exception;

import com.relyon.parkhere.dto.response.NearbySpotConflictResponse;
import com.relyon.parkhere.service.LocalizedMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LocalizedMessageService messageService;

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        var message = messageService.translate(ex);
        log.warn("Registration attempt with existing email: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        var message = messageService.translate(ex);
        log.warn("Failed login attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(NearbySpotExistsException.class)
    public ResponseEntity<NearbySpotConflictResponse> handleNearbySpotExists(NearbySpotExistsException ex) {
        var message = messageService.translate(ex);
        log.info("Spot creation blocked — {} nearby spots found", ex.getNearbySpots().size());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new NearbySpotConflictResponse(message, ex.getNearbySpots()));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidResetToken(InvalidResetTokenException ex) {
        var message = messageService.translate(ex);
        log.warn("Invalid password reset token");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(ReportCooldownException.class)
    public ResponseEntity<ErrorResponse> handleReportCooldown(ReportCooldownException ex) {
        var message = messageService.translate(ex);
        log.warn("Report cooldown triggered");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(UnauthorizedSpotModificationException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedSpotModification(UnauthorizedSpotModificationException ex) {
        var message = messageService.translate(ex);
        log.warn("Unauthorized spot modification attempt");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(RemovalRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRemovalRequestNotFound(RemovalRequestNotFoundException ex) {
        var message = messageService.translate(ex);
        log.warn("Removal request not found: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(DuplicateRemovalRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRemovalRequest(DuplicateRemovalRequestException ex) {
        var message = messageService.translate(ex);
        log.warn("Duplicate removal request");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(AlreadyConfirmedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyConfirmed(AlreadyConfirmedException ex) {
        var message = messageService.translate(ex);
        log.warn("Already confirmed removal request");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(SelfConfirmationException.class)
    public ResponseEntity<ErrorResponse> handleSelfConfirmation(SelfConfirmationException ex) {
        var message = messageService.translate(ex);
        log.warn("Self-confirmation attempt on removal request");
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(AlreadyFavoritedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyFavorited(AlreadyFavoritedException ex) {
        var message = messageService.translate(ex);
        log.warn("Spot already favorited");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(FavoriteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFavoriteNotFound(FavoriteNotFoundException ex) {
        var message = messageService.translate(ex);
        log.warn("Favorite not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        var message = messageService.translate(ex);
        log.warn("Invalid current password");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        var message = messageService.translate(ex);
        log.warn("User not found: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        var message = messageService.translate(ex);
        log.warn("Report not found: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(SpotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSpotNotFound(SpotNotFoundException ex) {
        var message = messageService.translate(ex);
        log.warn("Parking spot not found: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(InvalidImageException ex) {
        var message = messageService.translate(ex);
        log.warn("Invalid image upload attempt");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(TooManyImagesException.class)
    public ResponseEntity<ErrorResponse> handleTooManyImages(TooManyImagesException ex) {
        var message = messageService.translate(ex);
        log.warn("Too many images in report");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(ImageNotFoundException ex) {
        var message = messageService.translate(ex);
        log.warn("Image not found: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), message, LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid"));
        var message = messageService.translate("validation.failed");
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, LocalDateTime.now(), errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        var message = messageService.translate("error.internal");
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, LocalDateTime.now()));
    }

    public record ErrorResponse(int status, String message, LocalDateTime timestamp, Map<String, String> errors) {
        public ErrorResponse(int status, String message, LocalDateTime timestamp) {
            this(status, message, timestamp, null);
        }
    }
}
