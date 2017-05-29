package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Message;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListMessagesResponse extends BaseResponse {

    private List<Message> messages;

    public ListMessagesResponse() {
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "ListMessagesResponse{" +
                "messages=" + messages +
                "} " + super.toString();
    }
}
