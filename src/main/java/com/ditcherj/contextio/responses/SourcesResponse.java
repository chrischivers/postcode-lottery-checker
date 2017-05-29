package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Source;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourcesResponse extends BaseResponse {

    private List<Source> sources;

    public SourcesResponse() {
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    @Override
    public String toString() {
        return "SourcesResponse{" +
                "sources=" + sources +
                "} " + super.toString();
    }
}
