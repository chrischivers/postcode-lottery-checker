package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Match;
import com.ditcherj.contextio.dto.Query;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Author: Jonathan Ditcher
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListContactsResponse extends BaseResponse {

	private Query query;
	private List<Match> matches;

	public ListContactsResponse() {
	}

	public List<Match> getMatches() {
		return matches;
	}

	public void setMatches(List<Match> matches) {
		this.matches = matches;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

    @Override
    public String toString() {
        return "ListContactsResponse{" +
                "query=" + query +
                ", matches=" + matches +
                "} " + super.toString();
    }
}
