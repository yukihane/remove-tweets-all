package jp.himeji_cs;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Urls {

    public static final String PREFIX = "https://mobile.twitter.com";

    /**
     * ログインする際のスタートページ。
     * {@code _mb_tk}クッキーを取得したいので最初にアクセスします。
     */
    public static final String TOP = PREFIX + "/login";

    /**
     * ID, passwordをPOSTするURL。
     */
    public static final String LOGIN_POST = PREFIX + "/sessions";
}
