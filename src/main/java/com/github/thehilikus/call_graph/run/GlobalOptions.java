package com.github.thehilikus.call_graph.run;

import picocli.CommandLine.Option;

import java.nio.file.Path;

/**
 * The command line options
 */
public class GlobalOptions {
    @Option(names= "--db-folder", description = "the database folder")
    Path databaseFolder = getDefaultDatabaseFolder();

    @Option(names = "--db-name", description = "the database name")
    String databaseName = "call-graphs";

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
