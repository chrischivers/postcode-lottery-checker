package com.ditcherj.contextio.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateConnectTokenResponse extends SimpleResponse {

    private String token;
    private String resource_url;
    private String browser_redirect_url;

    public CreateConnectTokenResponse() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getResource_url() {
        return resource_url;
    }

    public void setResource_url(String resource_url) {
        this.resource_url = resource_url;
    }

    public String getBrowser_redirect_url() {
        return browser_redirect_url;
    }

    public void setBrowser_redirect_url(String browser_redirect_url) {
        this.browser_redirect_url = browser_redirect_url;
    }

    @Override
    public String toString() {
        return "CreateConnectTokenResponse{" +
                "token='" + token + '\'' +
                ", resource_url='" + resource_url + '\'' +
                ", browser_redirect_url='" + browser_redirect_url + '\'' +
                "} " + super.toString();
    }
}
