package com.github.thehilikus.call_graph.run;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

import java.nio.file.Path;

/**
 * Entry point
 */
@Command(name = "call-graph", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, subcommands = {ProcessCommand.class, QueryCommand.class, HelpCommand.class})
public class Main {
    @Option(names= "--db-folder", description = "the database folder", scope = CommandLine.ScopeType.INHERIT)
    protected Path databaseFolder = getDefaultDatabaseFolder();

    @Option(names = "--db-name", description = "the database name", scope = CommandLine.ScopeType.INHERIT)
    protected String databaseName = "call-graphs";

    @Option(names= {"-v", "--verbose"}, description = "Specify multiple -v options to increase verbosity (e.g., -v, -vv, -vvv)", scope = CommandLine.ScopeType.INHERIT)
    private void applyLogLevel(boolean[] verbosity) {
        Logger topAppLogger = (Logger) LoggerFactory.getLogger("com.github.thehilikus.call_graph");
        switch (verbosity.length) {
            case 1 -> topAppLogger.setLevel(Level.DEBUG);
            case 2 -> topAppLogger.setLevel(Level.TRACE);
            case 3 -> ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.TRACE);
        }
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }

    private Path getDefaultDatabaseFolder() {
        String dataHome;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            dataHome = System.getenv("LOCALAPPDATA");
        } else {
            dataHome = System.getenv().getOrDefault("XDG_DATA_HOME", System.getProperty("user.home") + "/.local/share");
        }
        return Path.of(dataHome, "com.github.thehilikus", "call-graph-visualizer/db");
    }
}
