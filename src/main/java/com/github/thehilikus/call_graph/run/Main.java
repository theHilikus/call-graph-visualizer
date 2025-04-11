package com.github.thehilikus.call_graph.run;

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

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
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
