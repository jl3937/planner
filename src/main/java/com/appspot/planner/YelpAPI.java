package com.appspot.planner;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class YelpAPI {
  private static final String YELP_API_URL = "http://api.yelp.com/v2/search";
  private static final int SEARCH_LIMIT = 3;

  private static final String CONSUMER_KEY = "9rSBf8WSgoTtCKlIEXtfqQ";
  private static final String CONSUMER_SECRET = "0RELM77bqmWU0aNvHJjr42VUG98";
  private static final String TOKEN = "rly9OpQwCfTQhWrbiwCknGslRerwpaq_";
  private static final String TOKEN_SECRET = "LreZIPz5CmwjrRVWCgXGNnZy_fM";

  OAuthService service;
  Token accessToken;

  public YelpAPI() {
    this.service = new ServiceBuilder()
        .provider(TwoStepOAuth.class)
	.apiKey(CONSUMER_KEY)
        .apiSecret(CONSUMER_SECRET)
	.build();
    this.accessToken = new Token(TOKEN, TOKEN_SECRET);
  }

  public String search(String term, String location) {
    OAuthRequest request = new OAuthRequest(Verb.GET, YELP_API_URL);
    request.addQuerystringParameter("term", term);
    request.addQuerystringParameter("location", location);
    request.addQuerystringParameter("limit", String.valueOf(SEARCH_LIMIT));
    return sendRequestAndGetResponse(request);
  }

  private String sendRequestAndGetResponse(OAuthRequest request) {
    this.service.signRequest(this.accessToken, request);
    Response response = request.send();
    return response.getBody();
  }
}
