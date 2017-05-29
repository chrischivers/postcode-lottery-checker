package com.ditcherj.contextio.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlagsResponse extends BaseResponse {

    private Boolean seen;
    private Boolean answered;
    private Boolean flagged;
    private Boolean deleted;
    private Boolean draft;

    public FlagsResponse() {
    }

    public Boolean getSeen() {
        return seen;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Boolean getFlagged() {
        return flagged;
    }

    public void setFlagged(Boolean flagged) {
        this.flagged = flagged;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

    @Override
    public String toString() {
        return "FlagsResponse{" +
                "seen=" + seen +
                ", answered=" + answered +
                ", flagged=" + flagged +
                ", deleted=" + deleted +
                ", draft=" + draft +
                "} " + super.toString();
    }
}
