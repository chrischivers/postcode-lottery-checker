package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    private String accountId;
    private Address addresses;
   // private PersonInfo person_info;
    private String message_id;
    private String email_message_id;
    private String gmail_message_id;
    private String gmail_thread_id;
    private List<File> files;
    private Long date;
    private Long date_indexed;
    private Long date_received;
    private String subject;
    private List<String> folders;
    private List<Source> sources;

    public Message() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Address getAddresses() {
        return addresses;
    }

    public void setAddresses(Address addresses) {
        this.addresses = addresses;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getEmail_message_id() {
        return email_message_id;
    }

    public void setEmail_message_id(String email_message_id) {
        this.email_message_id = email_message_id;
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

    public Long getDate_received() {
        return date_received;
    }

    public void setDate_received(Long date_received) {
        this.date_received = date_received;
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
        return "Message{" +
                "accountId='" + accountId + '\'' +
                ", addresses=" + addresses +
                ", message_id='" + message_id + '\'' +
                ", email_message_id='" + email_message_id + '\'' +
                ", gmail_message_id='" + gmail_message_id + '\'' +
                ", gmail_thread_id='" + gmail_thread_id + '\'' +
                ", files=" + files +
                ", date=" + date +
                ", date_indexed=" + date_indexed +
                ", date_received=" + date_received +
                ", subject='" + subject + '\'' +
                ", folders=" + folders +
                ", sources=" + sources +
                '}';
    }
}
