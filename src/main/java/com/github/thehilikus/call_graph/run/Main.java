package com.github.thehilikus.call_graph.run;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * Entry point
 */
@Command(name = "call-graph", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class, subcommands = {ProcessCommand.class, QueryCommand.class, HelpCommand.class})
public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
