package com.fidd.bench;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class RandomAccessEncryptionAlgorithmBenchmark
    extends AbstractRandomAccessEncryptionAlgorithmBenchmark {}
