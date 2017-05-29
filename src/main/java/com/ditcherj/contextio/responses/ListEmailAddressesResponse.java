package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.deserializers.EmailAddressesDeserializer;
import com.ditcherj.contextio.dto.EmailAddress;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 08/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonDeserialize(using = EmailAddressesDeserializer.class)
public class ListEmailAddressesResponse extends BaseResponse {

    private List<EmailAddress> emailAddresses;

    public ListEmailAddressesResponse() {
    }

    public List<EmailAddress> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<EmailAddress> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    @Override
    public String toString() {
        return "EmailAddressesResponse{" +
                "emailAddresses=" + emailAddresses +
                "} " + super.toString();
    }
}
