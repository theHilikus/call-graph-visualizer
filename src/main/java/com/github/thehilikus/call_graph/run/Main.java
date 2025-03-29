package com.github.thehilikus.call_graph.run;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;

/**
 * Entry point
 */
public class Main {
    public static void main(String[] args) {
        CliOptions options = parseArguments(args);
        Command command = options.command;

        command.execute(options);
    }

    private static CliOptions parseArguments(String[] args) {
        CliOptions result = new CliOptions();
        ParserProperties defaults = ParserProperties.defaults();
        final int usageWidth = 150;
        defaults.withUsageWidth(usageWidth);
        CmdLineParser parser = new CmdLineParser(result, defaults);
        try {
            parser.parseArgument(args);
            return result;
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
        }

        throw new IllegalArgumentException("Failed to parse arguments");
    }
}
