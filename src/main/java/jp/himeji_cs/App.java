package jp.himeji_cs;

import java.io.IOException;

public class App {

    public static void main(final String[] args) throws IOException {

        final TwitterService tw = new TwitterService();
        tw.init();
        tw.login("test", "testpw");
    }
}
