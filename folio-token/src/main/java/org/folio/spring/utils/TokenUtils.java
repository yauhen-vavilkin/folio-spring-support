package org.folio.spring.utils;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.model.UserToken;

@UtilityClass
public class TokenUtils {

  public static final String FOLIO_ACCESS_TOKEN = "folioAccessToken";
  public static final String FOLIO_REFRESH_TOKEN = "folioRefreshToken";

  public static String encodeRefreshTokenToCookie(String token, Instant expiration) {
    var ttlSeconds = expiration.getEpochSecond() - Instant.now().getEpochSecond();
    var cookie = new DefaultCookie(FOLIO_REFRESH_TOKEN, token);
    cookie.setMaxAge(ttlSeconds);
    cookie.setSecure(true);
    cookie.setPath("/authn");
    cookie.setHttpOnly(true);
    cookie.setSameSite(CookieHeaderNames.SameSite.None);
    cookie.setDomain(null);
    return cookie.toString();
  }

  public static UserToken parseUserTokenFromCookies(List<String> cookieHeaders,
                                                    AuthnClient.LoginResponse loginResponse) {
    var cookies =
        cookieHeaders
            .stream()
            .map(String::trim)
            .map(ServerCookieDecoder.STRICT::decodeAll)
            .flatMap(Collection::stream).toList();

    var accessToken = getTokenFromCookies(FOLIO_ACCESS_TOKEN, cookies);

    return UserToken.builder()
        .accessToken(accessToken)
        .accessTokenExpiration(parseExpiration(loginResponse.accessTokenExpiration()))
        .build();
  }

  private String getTokenFromCookies(String cookieName, List<Cookie> cookies) {
    return cookies
        .stream()
        .filter(cookie -> cookieName.equals(cookie.name()))
        .findFirst().map(Cookie::value)
        .orElseThrow(() -> new IllegalArgumentException("No cookie found for name: " + cookieName));
  }

  public Instant parseExpiration(String expireDate) {
    try {
      return Instant.parse(expireDate);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Unable to parse token expiration: " + expireDate);
    }
  }
}
