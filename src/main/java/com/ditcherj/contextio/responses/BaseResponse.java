package com.ditcherj.contextio.responses;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
public abstract class BaseResponse {

    private int code;
    private String error;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "code=" + code +
                ", error='" + error + '\'' +
                '}';
    }
}
