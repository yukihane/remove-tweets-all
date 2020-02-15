package jp.himeji_cs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @see <a href=
 * "https://help.twitter.com/ja/managing-your-account/how-to-download-your-twitter-archive">
 * 全ツイート履歴をダウンロードして見る方法</a>
 * @see <a href=
 * "https://help.twitter.com/en/managing-your-account/how-to-download-your-twitter-archive">
 * How to download your Twitter archive</a>
 */
@Slf4j
public class TwitterArchive {

    private static final Pattern PATTERN = Pattern.compile("    \"id\" : \"(\\d+)\",");

    private static Optional<String> resolve(final String str) {
        final Matcher m = PATTERN.matcher(str);
        if (m.matches()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    public List<String> getIds(final String fileName) throws IOException {
        final Path inPath = Paths.get(fileName);
        try (final ZipInputStream zis = new ZipInputStream(Files.newInputStream(inPath), StandardCharsets.UTF_8)) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if ("tweet.js".equals(ze.getName())) {
                    final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(zis, StandardCharsets.UTF_8));
                    final List<String> res = reader.lines()
                        .map(TwitterArchive::resolve)
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList());

                    log.trace("delete targets: {}", res);

                    return res;
                }
            }
        }

        throw new IOException("Invalid form file: " + fileName);
    }
}
