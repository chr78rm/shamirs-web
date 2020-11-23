/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Developer
 */
public class Database implements Traceable {

    void execute(File batch) throws IOException, InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "execute(File batch)");

        try {
            Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));
            ProcessBuilder processBuilder = new ProcessBuilder("mysql", "--defaults-extra-file=shamir-db.user.ini", "--verbose");
            
            processBuilder.environment().entrySet().forEach(entry -> tracer.out().printfIndentln("%s = %s", entry.getKey(), entry.getValue()));
            
            File workingDir = baseDir.resolve(Path.of("..", "sql", "mariadb")).toFile();
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

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }
}
