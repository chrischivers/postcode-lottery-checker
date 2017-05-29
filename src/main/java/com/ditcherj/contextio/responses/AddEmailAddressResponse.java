package com.ditcherj.contextio.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 08/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AddEmailAddressResponse extends SimpleResponse {

    private String email_address;
    private String resource_url;

    public AddEmailAddressResponse() {
    }

    public String getEmail_address() {
        return email_address;
    }

    public void setEmail_address(String email_address) {
        this.email_address = email_address;
    }

    public String getResource_url() {
        return resource_url;
    }

    public void setResource_url(String resource_url) {
        this.resource_url = resource_url;
    }

    @Override
    public String toString() {
        return "AddEmailAddressResponse{" +
                "email_address='" + email_address + '\'' +
                ", resource_url='" + resource_url + '\'' +
                "} " + super.toString();
    }
}
