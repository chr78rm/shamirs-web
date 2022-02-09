package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Developer
 */
abstract public class MariaDB extends Database {
    
    final List<String> command;

    public MariaDB(List<String> command) {
        this.command = command;
    }

    @Override
    void execute(File batch) throws IOException, InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "execute(File batch)");

        try {
            Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));
            ProcessBuilder processBuilder = new ProcessBuilder(this.command);
            
            processBuilder.environment().entrySet().forEach(entry -> tracer.out().printfIndentln("%s = %s", entry.getKey(), entry.getValue()));
            
            File workingDir = baseDir.resolve(Path.of("sql", "mariadb")).toFile();
            File logFile = baseDir.resolve(Path.of("log", "mariadb.log")).toFile();
            Process process = processBuilder.directory(workingDir)
                    .redirectInput(batch)
                    .redirectOutput(logFile)
                    .redirectError(logFile)
                    .start();
            
            tracer.out().printfIndentln("process.pid() = %d", process.pid());
            tracer.out().flush();
            
            final long TIMEOUT = 5;
            boolean exited = process.waitFor(TIMEOUT, TimeUnit.SECONDS);
            if (!exited) {
                throw new RuntimeException("Batch execution hangs.");
            }
        } finally {
            tracer.wayout();
        }
    }
    
}
