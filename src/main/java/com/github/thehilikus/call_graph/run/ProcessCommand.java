package com.github.thehilikus.call_graph.run;

import com.github.thehilikus.call_graph.db.GraphDatabase;
import com.github.thehilikus.call_graph.jar.JarAnalyzer;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Command to process a jar
 */
@Command(name = "process", description = "Process a jar to be able to query it later")
public class ProcessCommand implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessCommand.class);
    @Parameters(description = "the jar to process", arity = "1")
    private Path jarPath;

    @Option(names = "--dry-run", description = "Don't write to the database")
    private boolean dryRun = false;

    @Option(names="--truncate", description = "Deletes the existing database before creating a new one")
    private boolean truncate = false;

    @Option(names ="--include-packages", description = "Comma separated list of packages to include. If omitted, include everything")
    private Set<String> includePackages = null;

    @Option(names ="--exclude-packages", description = "Comma separated list of packages to exclude. If omitted, exclude JDK classes")
    private Set<String> excludePackages = Set.of("java.", "javax.", "sun.", "com.sun.", "jdk.", "org.w3c.dom.", "org.xml.sax.");

    @Mixin
    private GlobalOptions globalOptions;

    @Override
    public void run() {
        GraphDatabase db = new GraphDatabase(globalOptions.databaseFolder, globalOptions.databaseName);
        JarAnalyzer jarAnalyzer = new JarAnalyzer(jarPath, dryRun);

        if (truncate) {
            if (dryRun) {
                LOG.info("Not truncating database in dry-run mode");
            } else {
                db.truncate();
            }
        }
        db.initialize();
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.info("Start processing jar {}", jarPath);
        jarAnalyzer.process(db, new Filter(includePackages, excludePackages));
        LOG.info("Done processing jar in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
        db.shutdown();
    }
}
