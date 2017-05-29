package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Folder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FoldersResponse extends BaseResponse {

    private List<Folder> folders;

    public FoldersResponse() {
    }

    public List<Folder> getFolders() {
        return folders;
    }

    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }

    @Override
    public String toString() {
        return "FoldersResponse{" +
                "folders=" + folders +
                "} " + super.toString();
    }
}
