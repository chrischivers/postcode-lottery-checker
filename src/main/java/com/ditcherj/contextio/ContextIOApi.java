package com.ditcherj.contextio;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1RequestToken;

/**
 * Created by Jonathan Ditcher on 01/04/2017.
 */
public class ContextIOApi extends DefaultApi10a {

	@Override
	public String getAccessTokenEndpoint() {
		throw new UnsupportedOperationException("No endpoint for access token");
	}

	@Override
	public String getRequestTokenEndpoint() {
		throw new UnsupportedOperationException("No endpoint for request token");
	}

	@Override
	public String getAuthorizationUrl(OAuth1RequestToken oAuth1RequestToken) {
		throw new UnsupportedOperationException("No authorization url");
	}
}
