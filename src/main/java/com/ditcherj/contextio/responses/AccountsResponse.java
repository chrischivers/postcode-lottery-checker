package com.ditcherj.contextio.responses;

import com.ditcherj.contextio.dto.Account;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AccountsResponse extends BaseResponse {

    private List<Account> accounts;

    public AccountsResponse() {
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public String toString() {
        return "AccountsResponse{" +
                "accounts=" + accounts +
                "} " + super.toString();
    }
}
