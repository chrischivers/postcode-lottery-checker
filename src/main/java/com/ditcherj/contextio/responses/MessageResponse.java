package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Address;
import com.ditcherj.contextio.dto.File;
import com.ditcherj.contextio.dto.Source;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageResponse extends BaseResponse {

    private Long date;
    private Long date_indexed;
    private Address addresses;
    private String email_message_id;
    private String message_id;
    private String gmail_message_id;
    private String gmail_thread_id;
    private List<File> files;

    private String subject;
    private List<String> folders;
    private List<Source> sources;

    public MessageResponse() {
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public Long getDate_indexed() {
        return date_indexed;
    }

    public void setDate_indexed(Long date_indexed) {
        this.date_indexed = date_indexed;
    }

    public Address getAddresses() {
        return addresses;
    }

    public void setAddresses(Address addresses) {
        this.addresses = addresses;
    }

    public String getEmail_message_id() {
        return email_message_id;
    }

    public void setEmail_message_id(String email_message_id) {
        this.email_message_id = email_message_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getGmail_message_id() {
        return gmail_message_id;
    }

    public void setGmail_message_id(String gmail_message_id) {
        this.gmail_message_id = gmail_message_id;
    }

    public String getGmail_thread_id() {
        return gmail_thread_id;
    }

    public void setGmail_thread_id(String gmail_thread_id) {
        this.gmail_thread_id = gmail_thread_id;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void setFolders(List<String> folders) {
        this.folders = folders;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "date=" + date +
                ", date_indexed=" + date_indexed +
                ", addresses=" + addresses +
                ", email_message_id='" + email_message_id + '\'' +
                ", message_id='" + message_id + '\'' +
                ", gmail_message_id='" + gmail_message_id + '\'' +
                ", gmail_thread_id='" + gmail_thread_id + '\'' +
                ", files=" + files +
                ", subject=" + subject +
                ", folders=" + folders +
                ", sources=" + sources +
                "} " + super.toString();
    }
}
