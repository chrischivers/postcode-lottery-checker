package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {

    private Integer limit;
    private Integer offset;
    private Integer active_after;
    private Integer active_before;
    private String search;

    public Query() {
    }

    public Integer getActive_after() {
        return active_after;
    }

    public void setActive_after(Integer active_after) {
        this.active_after = active_after;
    }

    public Integer getActive_before() {
        return active_before;
    }

    public void setActive_before(Integer active_before) {
        this.active_before = active_before;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    @Override
    public String toString() {
        return "Query{" +
                "active_after=" + active_after +
                ", limit=" + limit +
                ", offset=" + offset +
                ", active_before=" + active_before +
                ", search='" + search + '\'' +
                '}';
    }
}
