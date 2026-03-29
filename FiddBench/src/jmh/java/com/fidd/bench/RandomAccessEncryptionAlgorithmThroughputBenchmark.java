package com.fidd.bench;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class RandomAccessEncryptionAlgorithmThroughputBenchmark
    extends AbstractRandomAccessEncryptionAlgorithmBenchmark {}
