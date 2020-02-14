package jp.himeji_cs;

import java.io.IOException;
import org.apache.hc.core5.http.ParseException;

public class App {

    public static void main(final String[] args) throws IOException, ParseException {

        final TwitterService tw = new TwitterService();
        tw.init();
        tw.login("test", "testpw");
        tw.deleteTweet("1228420678212186113");
    }
}
