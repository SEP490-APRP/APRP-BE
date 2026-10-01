package com.sang.be_capnong.exception;

import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.sang.be_capnong.dto.response.ApiResponse;

import java.util.Map;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";
    private static final String MAX_ATTRIBUTE = "max";

    @ExceptionHandler(value = NoResourceFoundException.class)
    ResponseEntity<ApiResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("No static resource found: {}", ex.getResourcePath());
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(404);
        apiResponse.setMessage("Resource not found: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handleException(Exception ex) {
        log.error("Unhandled Exception: ", ex);
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException ex) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getStatusCode()).body(
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        var fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : null;

        ApiResponse apiResponse = new ApiResponse();

        // 1. Message là tên enum ErrorCode (kiểu cũ) -> giữ hành vi cũ
        if (message != null) {
            try {
                ErrorCode errorCode = ErrorCode.valueOf(message);
                Map<String, Object> attributes = null;
                try {
                    var cv = fieldError.unwrap(ConstraintViolation.class);
                    attributes = cv.getConstraintDescriptor().getAttributes();
                } catch (Exception ignored) { }

                apiResponse.setCode(errorCode.getCode());
                apiResponse.setMessage(Objects.nonNull(attributes)
                        ? mapAttribute(errorCode.getMessage(), attributes)
                        : errorCode.getMessage());
                return ResponseEntity.badRequest().body(apiResponse);
            } catch (IllegalArgumentException ignored) {
                // không phải tên enum -> xuống bước 2
            }
        }

        // 2. Message là text thường -> trả 400 với đúng message
        apiResponse.setCode(400);
        apiResponse.setMessage(message != null ? message : ErrorCode.INVALID_KEY.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue =  String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
