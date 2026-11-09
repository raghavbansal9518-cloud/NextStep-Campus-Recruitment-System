package com.nextstep.util;

/**
 * Result<T>
 * 
 * Minimal generic container encapsulating either a successful operation containing data
 * of type T or a failure containing an error message.
 * 
 * Course Handout (25CSE0202) Syllabus Mapping:
 * - Generics: Lectures 47–48 (Class-level type parameter <T> and generic factory methods).
 * 
 * @param <T> The payload data type
 */
public class Result<T> {

    private final boolean success;
    private final T data;
    private final String message;

    private Result(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /**
     * Factory method for successful results.
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null);
    }

    /**
     * Factory method for failure results with an error message.
     */
    public static <T> Result<T> failure(String message) {
        return new Result<>(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return success ? "Result[Success: " + data + "]" : "Result[Failure: " + message + "]";
    }
}
