package jp.himeji_cs;

import java.io.IOException;
import java.util.List;
import jp.himeji_cs.exception.TargetNotFoundException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

@Slf4j
public class App {

    @Data
    static class Opt {

        @Option(names = "-u", description = "username or email for login", required = true)
        private String username;

        @Option(names = "-p", description = "password for login", required = true)
        private String password;

        @Option(names = "-f", description = "Twitter archive file(.zip)", required = true)
        private String archiveFile;

        @Option(names = "--dry-run", description = "true, if not delete actually")
        private boolean dryRun;

    }

    public static void main(final String[] args) throws IOException, ParseException {
        log.trace("Called: main");

        final Opt opt = new Opt();
        new CommandLine(opt).parseArgs(args);

        final TwitterArchive ar = new TwitterArchive();
        final List<String> targets = ar.getIds(opt.getArchiveFile());

        final TwitterService tw = new TwitterService();
        tw.init();
        tw.login(opt.getUsername(), opt.getPassword());

        if (opt.isDryRun()) {
            log.info("Successrul dry-run. See log for delete target IDs.");

            log.debug("DELETE TARGETS:");
            log.debug("total: {}", targets.size());
            targets.forEach(id -> {
                log.debug(id);
            });

            return;
        }

        targets.forEach(id -> {
            try {
                tw.deleteTweet(id);
            } catch (ParseException | IOException e) {
                log.error("Delete error, ID: {}, message: {}", id, e.getMessage());
                log.debug("", e);
            } catch (final TargetNotFoundException e) {
                log.error("Delete error(not found), ID: {}, message: {}", id, e.getMessage());
                log.debug("", e);
            }
        });
    }
}
