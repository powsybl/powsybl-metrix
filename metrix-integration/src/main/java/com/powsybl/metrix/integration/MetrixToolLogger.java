package com.powsybl.metrix.integration;

import com.google.common.base.Stopwatch;

import java.io.PrintStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MetrixToolLogger implements MetrixChunkLogger {

    private final PrintStream out;

    private final Stopwatch stopwatch = Stopwatch.createUnstarted();

    public MetrixToolLogger(PrintStream out) {
        this.out = Objects.requireNonNull(out);
    }

    @Override
    public void beforeNetworkWriting() {
        stopwatch.reset();
        out.println("Writing DIE...");
        stopwatch.start();
    }

    @Override
    public void afterNetworkWriting() {
        stopwatch.stop();
        out.println("DIE written in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }

    @Override
    public void beforeVariantsWriting() {
        stopwatch.reset();
        out.println("Writing variants...");
        stopwatch.start();
    }

    @Override
    public void afterVariantsWriting(int variantCount) {
        stopwatch.stop();
        out.println(variantCount + " variants written in " +
                stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }

    @Override
    public void beforeMetrixExecution() {
        stopwatch.reset();
        out.println("Running Metrix...");
        stopwatch.start();
    }

    @Override
    public void afterMetrixExecution() {
        stopwatch.stop();
        out.println("Metrix ran in " + stopwatch.elapsed(TimeUnit.SECONDS) + " s");
    }

    @Override
    public void beforeResultParsing() {
        stopwatch.reset();
        out.println("Parsing results...");
        stopwatch.start();
    }

    @Override
    public void afterResultParsing(int resultCount) {
        stopwatch.stop();
        out.println(resultCount + " results parsed in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }
}
