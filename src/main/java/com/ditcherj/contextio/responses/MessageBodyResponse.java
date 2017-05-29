package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.MessageBody;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBodyResponse extends BaseResponse {

    private List<MessageBody> messageBodies;

    public MessageBodyResponse() {
    }

    public List<MessageBody> getMessageBodies() {
        return messageBodies;
    }

    public void setMessageBodies(List<MessageBody> messageBodies) {
        this.messageBodies = messageBodies;
    }

    @Override
    public String toString() {
        return "MessageBodyResponse{" +
                "messageBodies=" + messageBodies +
                "} " + super.toString();
    }
}
