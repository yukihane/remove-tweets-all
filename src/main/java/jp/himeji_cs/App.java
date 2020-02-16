package jp.himeji_cs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        @Option(names = "-f", description = "Twitter archive file(.zip)")
        private String archiveFile;

        @Option(names = "--dry-run", description = "true, if not delete actually")
        private boolean dryRun;

    }

    public static void main(final String[] args) throws IOException, ParseException {
        log.trace("Called: main");
        log.debug("debug");
        try {
            exec(args);
        } catch (final Exception e) {
            log.debug("exception", e);
            throw e;
        }
    }

    private static void exec(final String[] args) throws IOException {

        final Opt opt = new Opt();
        new CommandLine(opt).parseArgs(args);

        final TwitterArchive ar = new TwitterArchive();
        final List<String> targets;
        final Optional<List<String>> stored = ar.readStore();
        if (!stored.isEmpty()) {
            targets = stored.get();
        } else {
            final String file = opt.getArchiveFile();
            if (file == null) {
                System.err.println("-f [archiveFile] option required");
                return;
            }
            targets = ar.getIds(opt.getArchiveFile());
        }

        final TwitterService tw = new TwitterService();
        tw.init();
        tw.login(opt.getUsername(), opt.getPassword());

        if (opt.isDryRun()) {
            log.info("Successful dry-run. See log for delete target IDs.");

            log.debug("DELETE TARGETS:");
            log.debug("total: {}", targets.size());
            targets.forEach(id -> {
                log.debug(id);
            });

            return;
        }

        final List<String> remains = new ArrayList<>(targets);
        log.info("remained size: {}", remains.size());
        try {

            targets.forEach(id -> {
                try {
                    tw.deleteTweet(id);
                    remains.remove(id);
                } catch (ParseException | IOException e) {
                    remains.remove(id);
                    log.error("Delete error, ID: {}, message: {}", id, e.getMessage());
                    log.debug("", e);
                } catch (final TargetNotFoundException e) {
                    remains.remove(id);
                    log.error("Delete error(not found), ID: {}", id);
                    log.debug("", e);
                }
            });
        } finally {
            ar.store(remains);
            log.info("remained size: {}", remains.size());
        }
        log.info("Finish deleting");
    }
}
