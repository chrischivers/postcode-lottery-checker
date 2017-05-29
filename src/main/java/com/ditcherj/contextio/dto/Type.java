package com.ditcherj.contextio.dto;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
public enum Type {

    TXT("text/plain"),
    HTML("text/html");

    private String mimeType;

    Type(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
