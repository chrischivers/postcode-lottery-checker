package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.ConnectToken;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectTokensResponse extends BaseResponse {

    private List<ConnectToken> tokens;

    public ConnectTokensResponse() {
    }

    public List<ConnectToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<ConnectToken> tokens) {
        this.tokens = tokens;
    }

    @Override
    public String toString() {
        return "ConnectTokensResponse{" +
                "tokens=" + tokens +
                "} " + super.toString();
    }
}
