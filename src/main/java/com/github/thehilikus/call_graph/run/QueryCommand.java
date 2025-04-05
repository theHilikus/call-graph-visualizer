package com.github.thehilikus.call_graph.run;

import com.github.thehilikus.call_graph.browser.BrowserException;
import com.github.thehilikus.call_graph.browser.BrowserServer;
import com.github.thehilikus.call_graph.db.GraphDatabase;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Command to query the database
 */
public class QueryCommand implements Command {
    private static final Logger LOG = LoggerFactory.getLogger(QueryCommand.class);
    @SuppressWarnings("MagicNumber")
    @Option(name = "--bolt-port", usage = "the bolt port to use")
    private Integer boltPort = 7687;

    @Option(name = "--browser-port", usage = "the neo4j-browser port to use")
    private int browserPort = 8080;

    @Override
    public void execute(CliOptions globalOptions) {
        GraphDatabase db = new GraphDatabase(globalOptions.databaseFolder, globalOptions.databaseName);
        db.initialize(boltPort);

        BrowserServer browser = new BrowserServer(browserPort);
        LOG.info("Starting neo4j-browser");
        browser.start();
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:" + browserPort));
        } catch (IOException | URISyntaxException e) {
            throw new BrowserException("Error launching browser", e);
        }
    }
}
