package com.github.thehilikus.call_graph.run;

import com.github.thehilikus.call_graph.browser.BrowserException;
import com.github.thehilikus.call_graph.browser.BrowserServer;
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
    private Integer boltPort = 7687;

    @SuppressWarnings("MagicNumber")
    @Option(names = "--browser-port", description = "the neo4j-browser port to use")
    private int browserPort = 8080;

    @ParentCommand
    private Main main;

    @Override
    public void run() {
        GraphDatabase db = new GraphDatabase(main.databaseFolder, main.databaseName);
        db.initialize(boltPort);

        BrowserServer browser = new BrowserServer(browserPort);
        LOG.info("Starting neo4j-browser");
        browser.start();
        try {
            Desktop.getDesktop().browse(new URI("http://localhost:" + browserPort));
            browser.join();
        } catch (IOException | URISyntaxException e) {
            throw new BrowserException("Error launching browser", e);
        } catch (InterruptedException e) {
            LOG.info("Browser stopped by user");
        }
    }
}
