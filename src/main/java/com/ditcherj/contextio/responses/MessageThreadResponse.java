package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Message;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageThreadResponse extends BaseResponse {

    private List<String> email_message_ids;
    private List<Message> messages;

    public MessageThreadResponse() {
    }

    public List<String> getEmail_message_ids() {
        return email_message_ids;
    }

    public void setEmail_message_ids(List<String> email_message_ids) {
        this.email_message_ids = email_message_ids;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "MessageThreadResponse{" +
                "email_message_ids=" + email_message_ids +
                ", messages=" + messages +
                "} " + super.toString();
    }
}
