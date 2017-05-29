package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class File {

    private Long size;
    private String type;
    private String file_name;
    //private List<String> file_name_structure;
    private Double body_section;
    private String file_id;
    private Boolean is_embedded;
    private String content_disposition;
    private String content_id;

    public File() {
    }

    public Double getBody_section() {
        return body_section;
    }

    public void setBody_section(Double body_section) {
        this.body_section = body_section;
    }

    public String getContent_disposition() {
        return content_disposition;
    }

    public void setContent_disposition(String content_disposition) {
        this.content_disposition = content_disposition;
    }

    public String getContent_id() {
        return content_id;
    }

    public void setContent_id(String content_id) {
        this.content_id = content_id;
    }

    public String getFile_id() {
        return file_id;
    }

    public void setFile_id(String file_id) {
        this.file_id = file_id;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public Boolean getIs_embedded() {
        return is_embedded;
    }

    public void setIs_embedded(Boolean is_embedded) {
        this.is_embedded = is_embedded;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "File{" +
                "body_section=" + body_section +
                ", size=" + size +
                ", type='" + type + '\'' +
                ", file_name='" + file_name + '\'' +
                ", file_id='" + file_id + '\'' +
                ", is_embedded=" + is_embedded +
                ", content_disposition='" + content_disposition + '\'' +
                ", content_id='" + content_id + '\'' +
                '}';
    }

}
