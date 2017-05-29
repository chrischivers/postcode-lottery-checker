package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Flags;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SetFlagsResponse extends SimpleResponse {

    private Flags flags;

    public SetFlagsResponse() {
    }

    public Flags getFlags() {
        return flags;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "SetFlagsResponse{" +
                "flags=" + flags +
                "} " + super.toString();
    }
}
