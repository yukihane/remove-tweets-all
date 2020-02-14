package jp.himeji_cs;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

@Slf4j
public class TwitterService {
    private final CloseableHttpClient client;
    private HttpCookie mbTkCookie;

    public TwitterService() {
        final RequestConfig globalConfig = RequestConfig.custom()
            .setCookieSpec(StandardCookieSpec.RELAXED)
            .setRedirectsEnabled(false)
            .build();
        client = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
        //        client = HttpClients.createDefault();
    }

    public void init() throws IOException {
        log.debug("Called: init");

        final HttpGet httpget = new HttpGet(Urls.TOP);
        try (final CloseableHttpResponse resp = client.execute(httpget)) {
            mbTkCookie = extractCookie(resp, "_mb_tk").orElseThrow();
        }
        System.out.println(toString(mbTkCookie));
    }

    private static String toString(final HttpCookie e) {
        return String.format("name=%s,value=%s,domain=%s,maxage=%s,secure=%s,httpOnly=%s",
            e.getName(), e.getValue(), e.getDomain(), e.getMaxAge(), e.getSecure(), e.isHttpOnly());
    }

    public void login(final String id, final String password) throws IOException {
        log.debug("Called: login");

        final List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("authenticity_token", mbTkCookie.getValue()));
        nvps.add(new BasicNameValuePair("session[username_or_email]", id));
        nvps.add(new BasicNameValuePair("session[password]", password));

        final HttpPost httpPost = new HttpPost(Urls.LOGIN_POST);
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpResponse resp = client.execute(httpPost)) {
            extractCookie(resp, "auth_token")
                .orElseThrow(() -> new RuntimeException("Login failed"));
        }
    }

    private static Optional<HttpCookie> extractCookie(final HttpResponse resp, final String name) {

        final Optional<String> cookieStr = Arrays.stream(resp.getHeaders("set-cookie"))
            .map(Header::getValue)
            .filter(e -> e.startsWith(name))
            .findAny();

        log.debug("cookie({}): {}", name, cookieStr);
        return cookieStr.map(str -> HttpCookie.parse(str).get(0));
    }
}
