package com.github.thehilikus.call_graph.run;

import picocli.CommandLine;

/**
 * Pico-cli version provider used from "--version"
 */
public class VersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return new String[] { "${COMMAND-FULL-NAME} version " + version };
    }
}
