package com.ditcherj.contextio.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoveMessageResponse extends BaseResponse {

    private Boolean success;
    private String connection_log;

    public MoveMessageResponse() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "MoveMessageResponse{" +
                "success=" + success +
                ", connection_log='" + connection_log + '\'' +
                "} " + super.toString();
    }
}
