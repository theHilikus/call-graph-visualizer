package com.github.thehilikus.call_graph.run;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.nio.file.Path;

/**
 * The command line options
 */
public class CliOptions {
    @Argument(required = true, handler = SubCommandHandler.class, metaVar = "COMMAND", usage = "the command to execute")
    @SubCommands({
            @SubCommand(name = "process", impl = ProcessCommand.class),
            @SubCommand(name = "query", impl = QueryCommand.class)
    })
    Command command;

    @Option(name = "--db-folder", usage = "the database folder")
    Path databaseFolder = getDefaultDatabaseFolder();

    @Option(name = "--db-name", usage = "the database name", metaVar = "NAME")
    String databaseName = "call-graphs";

    @Option(name = "--dry-run", usage = "Don't write to the database")
    boolean dryRun = false;

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
