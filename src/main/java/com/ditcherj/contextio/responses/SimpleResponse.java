package com.ditcherj.contextio.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class SimpleResponse extends BaseResponse {

    private Boolean success;

    public SimpleResponse() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "PostMessageResponse{" +
                "success=" + success +
                "} " + super.toString();
    }
}
