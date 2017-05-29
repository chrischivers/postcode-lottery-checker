package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBody {

    private String type;
    private String charset;
    private String content;
    private String body_section;

    public MessageBody() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBody_section() {
        return body_section;
    }

    public void setBody_section(String body_section) {
        this.body_section = body_section;
    }

    @Override
    public String toString() {
        return "MessageBody{" +
                "type='" + type + '\'' +
                ", charset='" + charset + '\'' +
                ", content='" + content + '\'' +
                ", body_section='" + body_section + '\'' +
                '}';
    }
}
