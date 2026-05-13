package com.aerospace.production_service.common;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private List<String> errors;

    public ApiResponse(String status, T data) {
        this.status = status;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().status("SUCCESS")
                .message("Operation completed successfully")
                .data(data).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().status("SUCCESS")
                .message(message).data(data).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder().status("CREATED")
                .message("Resource created successfully")
                .data(data).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> updated(T data) {
        return ApiResponse.<T>builder().status("UPDATED")
                .message("Resource updated successfully")
                .data(data).timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> deleted() {
        return ApiResponse.<T>builder().status("DELETED")
                .message("Resource deleted successfully")
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().status("ERROR")
                .message(message).timestamp(LocalDateTime.now()).build();
    }
}
