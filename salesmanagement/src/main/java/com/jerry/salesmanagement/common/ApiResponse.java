package com.jerry.salesmanagement.common;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    // 失败响应
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(null);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }

    // 参数错误
    public static <T> ApiResponse<T> badRequest(String message) {
        return error(400, message);
    }

    // 未授权
    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(401, message);
    }

    // 禁止访问
    public static <T> ApiResponse<T> forbidden(String message) {
        return error(403, message);
    }

    // 资源不存在
    public static <T> ApiResponse<T> notFound(String message) {
        return error(404, message);
    }
}
