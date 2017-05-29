package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Source {

    private String server;
    private String label;
    private String username;
    private Integer port;
    private Boolean use_ssl;
    private String authentication_type;
    private SourceStatus status;
    private String status_message;
    private String status_callback_url;
    private String resource_url;
    private Boolean sync_flags;
    private String callback_url;
    private Boolean sync_all_folders;
    private Boolean expunge_on_deleted_flag;
    private Boolean raw_file_list;

    public Source() {
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Boolean getUse_ssl() {
        return use_ssl;
    }

    public void setUse_ssl(Boolean use_ssl) {
        this.use_ssl = use_ssl;
    }

    public String getAuthentication_type() {
        return authentication_type;
    }

    public void setAuthentication_type(String authentication_type) {
        this.authentication_type = authentication_type;
    }

    public SourceStatus getStatus() {
        return status;
    }

    public void setStatus(SourceStatus status) {
        this.status = status;
    }

    public String getStatus_message() {
        return status_message;
    }

    public void setStatus_message(String status_message) {
        this.status_message = status_message;
    }

    public String getStatus_callback_url() {
        return status_callback_url;
    }

    public void setStatus_callback_url(String status_callback_url) {
        this.status_callback_url = status_callback_url;
    }

    public String getResource_url() {
        return resource_url;
    }

    public void setResource_url(String resource_url) {
        this.resource_url = resource_url;
    }

    public Boolean getSync_flags() {
        return sync_flags;
    }

    public void setSync_flags(Boolean sync_flags) {
        this.sync_flags = sync_flags;
    }

    public String getCallback_url() {
        return callback_url;
    }

    public void setCallback_url(String callback_url) {
        this.callback_url = callback_url;
    }

    public Boolean getSync_all_folders() {
        return sync_all_folders;
    }

    public void setSync_all_folders(Boolean sync_all_folders) {
        this.sync_all_folders = sync_all_folders;
    }

    public Boolean getExpunge_on_deleted_flag() {
        return expunge_on_deleted_flag;
    }

    public void setExpunge_on_deleted_flag(Boolean expunge_on_deleted_flag) {
        this.expunge_on_deleted_flag = expunge_on_deleted_flag;
    }

    public Boolean getRaw_file_list() {
        return raw_file_list;
    }

    public void setRaw_file_list(Boolean raw_file_list) {
        this.raw_file_list = raw_file_list;
    }

    @Override
    public String toString() {
        return "Source{" +
                "server='" + server + '\'' +
                ", label='" + label + '\'' +
                ", username='" + username + '\'' +
                ", port=" + port +
                ", use_ssl=" + use_ssl +
                ", authentication_type='" + authentication_type + '\'' +
                ", status='" + status + '\'' +
                ", status_message='" + status_message + '\'' +
                ", status_callback_url='" + status_callback_url + '\'' +
                ", resource_url='" + resource_url + '\'' +
                ", sync_flags=" + sync_flags +
                ", callback_url='" + callback_url + '\'' +
                ", sync_all_folders=" + sync_all_folders +
                ", expunge_on_deleted_flag=" + expunge_on_deleted_flag +
                ", raw_file_list=" + raw_file_list +
                '}';
    }
}
