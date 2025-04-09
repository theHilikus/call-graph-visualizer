package com.github.thehilikus.call_graph.browser;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.util.resource.Resource;

import java.net.URL;

/**
 * Neo4j browser server
 */
public class BrowserServer {
    private static final Logger LOG = LoggerFactory.getLogger(BrowserServer.class);
    private static final String STATIC_RESOURCES_PATH = "/browser";
    private final int browserPort;
    private Server server;
    private Thread thread;

    public BrowserServer(int browserPort) {
        this.browserPort = browserPort;
    }

    public void start() {
        checkNeo4jBrowserIsAvailable();

        server = new Server(browserPort);
        ResourceHandler resourceHandler = createResourceHandler();
        Handler.Singleton contextHandler = new ContextHandler("/"); //server at the root of the server
        contextHandler.setHandler(resourceHandler);

        try {
            server.setHandler(contextHandler);
            LOG.info("Starting jetty server on port {}", browserPort);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (Exception e) {
            server.destroy();
            throw new BrowserException("Failed to start browser server", e);
        }
    }

    private ResourceHandler createResourceHandler() {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirAllowed(false);
        resourceHandler.setWelcomeFiles("index.html"); // Serve index.html by default

        Resource resource = ResourceFactory.of(resourceHandler).newClassLoaderResource(STATIC_RESOURCES_PATH);
        resourceHandler.setBaseResource(resource);
        return resourceHandler;
    }

    private void checkNeo4jBrowserIsAvailable() {
        URL checkUrl = BrowserServer.class.getResource(STATIC_RESOURCES_PATH);
        if (checkUrl == null) {
            throw new BrowserException("Resource path '" + STATIC_RESOURCES_PATH + "' not found in any JAR or directory on the classpath.");
        }
        LOG.debug("Found resource base: {}", checkUrl.toExternalForm());
    }

    private void stop() {
        LOG.info("Stopping jetty server");
        try {
            thread.interrupt();
            server.stop();
        } catch (Exception e) {
            throw new BrowserException("Failed to stop browser server", e);
        }
    }

    public void join() throws InterruptedException {
        thread = Thread.currentThread();
        server.join();
    }
}
