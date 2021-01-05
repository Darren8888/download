package com.darren.download.exception;

public class DownloadException extends RuntimeException {
    public static final int CODE_EXCEPTION_URL_NULL = 1;
    public static final int CODE_EXCEPTION_PATH_NULL = 2;
    public static final int CODE_EXCEPTION_URL_ERR = 3;
    public static final int CODE_EXCEPTION_SERVER_ERR = 4;
    public static final int CODE_EXCEPTION_PROTOCOL = 5;
    public static final int CODE_EXCEPTION_IO_ERR = 6;
    public static final int CODE_EXCEPTION_FILE_NULL = 7;
    public static final int CODE_EXCEPTION_PAUSE = 8;
    public static final int CODE_EXCEPTION_INIT_FAILED = 9;

    private int code = 0;

    public DownloadException(int code, String message) {
        super(message);
        this.code = code;
    }

    public DownloadException(String message) {
        super(message);
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("err code: ");
        builder.append(code);
        builder.append(", message: ");
        builder.append(this.getMessage());
        return builder.toString();
    }
}
