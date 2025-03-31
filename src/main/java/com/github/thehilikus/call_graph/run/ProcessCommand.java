package com.github.thehilikus.call_graph.run;

import com.github.thehilikus.call_graph.db.GraphDatabase;
import com.github.thehilikus.call_graph.jar.JarAnalyzer;
import org.apache.commons.lang3.time.StopWatch;
import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Command to process a jar
 */
public class ProcessCommand implements Command {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessCommand.class);
    @Argument(required = true, metaVar = "jar", usage = "the jar to process")
    private Path jarPath;

    @Override
    public void execute(CliOptions globalOptions) {
        GraphDatabase db = new GraphDatabase(globalOptions.databaseFolder, globalOptions.databaseName);
        JarAnalyzer jarAnalyzer = new JarAnalyzer(jarPath);

        db.initialize();
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.info("Start processing jar {}", jarPath);
        jarAnalyzer.process(db, globalOptions.dryRun);
        LOG.info("Done processing jar in {} ms", stopWatch.getTime(TimeUnit.MILLISECONDS));
        db.shutdown();
    }
}
