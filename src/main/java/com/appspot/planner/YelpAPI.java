import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class YelpAPI {
  private static final String YELP_API_URL = "http://api.yelp.com/v2/search";
  private static final String CONSUMER_KEY = "9rSBf8WSgoTtCKlIEXtfqQ";
  private static final String CONSUMER_SECRET = "0RELM77bqmWU0aNvHJjr42VUG98";
  private static final String TOKEN = "rly9OpQwCfTQhWrbiwCknGslRerwpaq_";
  private static final String TOKEN_SECRET = "LreZIPz5CmwjrRVWCgXGNnZy_fM";

  OAuthService service;
  Token accessToken;
}
