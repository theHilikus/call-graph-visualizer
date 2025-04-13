package com.github.thehilikus.call_graph.run;

import com.github.thehilikus.call_graph.browser.BrowserException;
import com.github.thehilikus.call_graph.db.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Command to query the database
 */
@Command(name = "query", description = "Query the database")
public class QueryCommand implements Runnable{
    private static final Logger LOG = LoggerFactory.getLogger(QueryCommand.class);
    @SuppressWarnings("MagicNumber")
    @Option(names = "--bolt-port", description = "the bolt port to use")
    private int boltPort = 7687;

    @SuppressWarnings("MagicNumber")
    @Option(names = "--http-port", description = "the neo4j-browser port to use")
    private int httpPort = 7474;

    @ParentCommand
    private Main main;

    @Override
    public void run() {
        LOG.info("Starting neo4j-browser");
        GraphDatabase db = new GraphDatabase(main.databaseFolder, main.databaseName);
        db.enableBrowser(boltPort, httpPort);
        db.initialize();
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:" + httpPort));
        } catch (IOException | URISyntaxException e) {
            throw new BrowserException("Error launching browser", e);
        }
    }
}
