package jp.himeji_cs;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jp.himeji_cs.exception.AuthorizationException;
import jp.himeji_cs.exception.TargetNotFoundException;
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
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.jsoup.Connection.KeyVal;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

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
                .orElseThrow(() -> new AuthorizationException("Login failed"));
        }
    }

    /**
     * 該当ツイートが削除可能かどうか。
     * これは、画面に削除ボタンが表示されるかどうかです。
     * 削除ボタンを押した時、本当に削除されるかどうかはわかりません。
     * (なんと、他人のツイートを表示した場合にも削除ボタンは表示されます！
     * が、もちろん削除ボタンを押しても削除はされません)
     */
    public void deleteTweet(final String tweetId) throws IOException, ParseException {
        log.debug("Called: deleteTweet: " + tweetId);

        final String url = Urls.DELETE_TMPLATE.replace("{id}", tweetId);

        final HttpGet statusPage = new HttpGet(url);
        //        statusPage.addHeader("sec-fetch-site", "same-origin");
        statusPage.addHeader("referer", url);

        final List<NameValuePair> nvps;
        try (final CloseableHttpResponse resp = client.execute(statusPage)) {
            final String body = EntityUtils.toString(resp.getEntity());
            log.trace("BODY: {}", body);
            final Document html = Jsoup.parse(body);
            final Elements forms = html.getElementsByTag("form");
            final int size = forms.size();
            if (size != 1) {
                throw new TargetNotFoundException(
                    String.format("Not found tweet(%d) %s", size, tweetId));
            }
            final FormElement form = (FormElement) forms.get(0);
            final List<KeyVal> formData = form.formData();

            log.trace("formData: {}", formData);

            final Optional<String> postKeyTweetId = formData.stream()
                .map(KeyVal::key)
                .filter("tweet_id"::equals)
                .findAny();
            if (postKeyTweetId.isEmpty()) {
                throw new TargetNotFoundException(
                    String.format("Not found tweet(%d) %s", size, tweetId));
            }

            nvps = formData.stream()
                .map(e -> new BasicNameValuePair(e.key(), e.value()))
                .collect(Collectors.toList());
        }

        final HttpPost deleteReq = new HttpPost(url);
        deleteReq.addHeader("referer", url);
        deleteReq.setEntity(new UrlEncodedFormEntity(nvps));

        try (final CloseableHttpResponse resp = client.execute(deleteReq)) {
            // 本当に削除しているかどうかはこのレスポンスではほとんど判明しないので気休め
            // 削除できたかちゃんと確認するには再度同じidでリクエストかけてみるしか無いと思う
            final int statusCode = resp.getCode();
            if (statusCode >= 400 && statusCode < 500) {
                throw new AuthorizationException(
                    String.format("Delete request(%s) is not accepted: %d",
                        tweetId, statusCode));
            } else if (statusCode >= 500) {
                throw new IOException("Delete error: " + statusCode);
            }
        }
    }

    private static Optional<HttpCookie> extractCookie(final HttpResponse resp, final String name) {

        final Optional<String> cookieStr = Arrays.stream(resp.getHeaders("set-cookie"))
            .map(Header::getValue)
            .filter(e -> e.startsWith(name))
            .findAny();

        log.trace("cookie({}): {}", name, cookieStr);
        return cookieStr.map(str -> HttpCookie.parse(str).get(0));
    }
}
