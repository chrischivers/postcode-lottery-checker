package com.ditcherj.contextio.dto;

/**
 * Created by Jonathan Ditcher on 08/04/2017.
 */
public class EmailAddress {

    private String email;
    private Long validated;
    private Boolean primary;
    private String resource_url;

    public EmailAddress() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getValidated() {
        return validated;
    }

    public void setValidated(Long validated) {
        this.validated = validated;
    }

    public Boolean getPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        this.primary = primary;
    }

    public String getResource_url() {
        return resource_url;
    }

    public void setResource_url(String resource_url) {
        this.resource_url = resource_url;
    }

    @Override
    public String toString() {
        return "EmailAddress{" +
                "email='" + email + '\'' +
                ", validated=" + validated +
                ", primary=" + primary +
                ", resource_url='" + resource_url + '\'' +
                '}';
    }
}
