package com.ditcherj.contextio.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 02/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {

    private Contact from;
    private List<Contact> to;
    private List<Contact> cc;


    public Address() {
    }

    public List<Contact> getCc() {
        return cc;
    }

    public void setCc(List<Contact> cc) {
        this.cc = cc;
    }

    public Contact getFrom() {
        return from;
    }

    public void setFrom(Contact from) {
        this.from = from;
    }

    public List<Contact> getTo() {
        return to;
    }

    public void setTo(List<Contact> to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "Addresses{" +
                "from=" + from +
                ", to=" + to +
                ", cc=" + cc +
                '}';
    }
}
