package com.github.thehilikus.call_graph.run;

import org.kohsuke.args4j.Argument;

import java.nio.file.Path;

/**
 * Command to process a jar
 */
public class ProcessCommand implements Command {
    @Argument(required = true, metaVar = "jar", usage = "the jar to process")
    private Path jarPath;

    @Override
    public void execute(CliOptions globalOptions) {

    }
}
