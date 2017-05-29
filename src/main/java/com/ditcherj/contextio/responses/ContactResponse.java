package com.ditcherj.contextio.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ContactResponse extends BaseResponse {

    private String email;
    private Integer count;
    private Integer sent_count;
    private Integer received_count;
    private Integer sent_from_account_count;
    private String name;
    private String thumbnail;
    private Long last_received;
    private Long last_sent;
    private List<String> emails;

    public ContactResponse() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
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

    public Long getLast_received() {
        return last_received;
    }

    public void setLast_received(Long last_received) {
        this.last_received = last_received;
    }

    public Long getLast_sent() {
        return last_sent;
    }

    public void setLast_sent(Long last_sent) {
        this.last_sent = last_sent;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    @Override
    public String toString() {
        return "ContactResponse{" +
                "email='" + email + '\'' +
                ", count=" + count +
                ", sent_count=" + sent_count +
                ", received_count=" + received_count +
                ", sent_from_account_count=" + sent_from_account_count +
                ", name='" + name + '\'' +
                ", thumbnail='" + thumbnail + '\'' +
                ", last_received=" + last_received +
                ", last_sent=" + last_sent +
                ", emails=" + emails +
                "} " + super.toString();
    }
}
