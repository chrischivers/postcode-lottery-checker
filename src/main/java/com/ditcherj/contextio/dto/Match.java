package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Match {

    private String email;
    private Integer count;
    private Integer sent_count;
    private Integer received_count;
    private Integer sent_from_account_count;
    private String name;
    private String thumbnail;

    public Match() {
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Integer getSent_count() {
        return sent_count;
    }

    public void setSent_count(Integer sent_count) {
        this.sent_count = sent_count;
    }

    public Integer getReceived_count() {
        return received_count;
    }

    public void setReceived_count(Integer received_count) {
        this.received_count = received_count;
    }

    public Integer getSent_from_account_count() {
        return sent_from_account_count;
    }

    public void setSent_from_account_count(Integer sent_from_account_count) {
        this.sent_from_account_count = sent_from_account_count;
    }

    @Override
    public String toString() {
        return "Match{" +
                "email='" + email + '\'' +
                ", count=" + count +
                ", sent_count=" + sent_count +
                ", received_count=" + received_count +
                ", sent_from_account_count=" + sent_from_account_count +
                ", name='" + name + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                '}';
    }
}
