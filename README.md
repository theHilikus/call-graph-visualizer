# Call Graph Visualizer

A Java application that analyzes JAR files to create, query, and visualize method call graphs. It uses Neo4j as the backend graph database and provides a web-based visualization interface.

## Features

- Analyzes Java bytecode in JAR files to extract method call relationships
- Stores call graph data in a Neo4j graph database
- Provides a web interface for visualizing and exploring call graphs
- Supports querying the call graph to find specific patterns and relationships
- Uses ASM for bytecode analysis

## Requirements

- Java 21 or higher
- Maven 3.x

## Building

```bash
mvn clean package
```

## Usage

1. Analyze a JAR file:
   ```bash
   java -jar target/call-graph-visualizer-1.0-SNAPSHOT.jar analyze path/to/your.jar
   ```

2. Query the call graph:
   ```bash
   java -jar target/call-graph-visualizer-1.0-SNAPSHOT.jar query [options]
   ```

3. Start the visualization server:
   ```bash
   java -jar target/call-graph-visualizer-1.0-SNAPSHOT.jar serve
   ```
   Then open your web browser to view the call graph visualization.

## Technologies

- Neo4j - Graph database
- ASM - Java bytecode manipulation and analysis
- Jetty - Embedded web server
- Neo4j Browser - Web-based visualization interface

## License

This project is licensed under the MIT License.
