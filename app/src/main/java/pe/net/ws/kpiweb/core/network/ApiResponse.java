package pe.net.ws.kpiweb.core.network;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("status")
    private String status;

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("error_code")
    private String errorCode;

    public ApiResponse() {}

    public ApiResponse(String status, int code, String message, T data, String errorCode) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", 200, "OK", data, null);
    }

    public static <T> ApiResponse<T> error(int code, String errorCode, String message) {
        return new ApiResponse<>("error", code, message, null, errorCode);
    }

    public boolean isSuccessful() {
        return "success".equalsIgnoreCase(status);
    }

    public String getStatus() { return status; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public String getErrorCode() { return errorCode; }
}
