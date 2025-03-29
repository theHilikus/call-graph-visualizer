package com.github.thehilikus.call_graph.run;

/**
 * One of the application commands
 */
public interface Command {
    /**
     * Executes the command with the provided global options.
     *
     * @param globalOptions the global options to be used during the execution of the command
     */
    void execute(CliOptions globalOptions);
}
