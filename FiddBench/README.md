# FiddBench

JMH benchmarks for `FiddCore` encryption algorithms.

## What is measured

This module focuses on algorithm-level performance.

Current benchmark groups:

- `EncryptionAlgorithmBenchmark`: average time (`us/op`) for basic `encrypt/decrypt` and stream crypto-only methods.
- `EncryptionAlgorithmThroughputBenchmark`: throughput (`ops/s`) for the same encryption methods.
- `RandomAccessEncryptionAlgorithmBenchmark`: average time (`us/op`) for random-access decrypt APIs.
- `RandomAccessEncryptionAlgorithmThroughputBenchmark`: throughput (`ops/s`) for the same random-access methods.

The benchmark state currently varies:

- algorithm
- payload size
- random-access offset
- random-access length

## Running benchmarks

Compile benchmark classes:

```bash
gradlew :FiddBench:jmhClasses
```

Run all benchmarks:

```bash
gradlew :FiddBench:jmh
```

Run only average-time benchmarks:

```bash
gradlew :FiddBench:jmh --tests "*EncryptionAlgorithmBenchmark*"
gradlew :FiddBench:jmh --tests "*RandomAccessEncryptionAlgorithmBenchmark*"
```

Run only throughput benchmarks:

```bash
gradlew :FiddBench:jmh --tests "*ThroughputBenchmark*"
```

## Result files

JMH results are written as JSON because `FiddBench/build.gradle` sets:

```gradle
jmh {
    resultFormat = 'JSON'
}
```

Typical output file:

- `FiddBench/build/results/jmh/results.json`

## Notes

- Stream benchmarks intentionally use `OutputStream.nullOutputStream()` to avoid measuring output buffering/allocation
  when the goal is crypto cost.
- Random-access benchmarks validate `offset/length` combinations during `@Setup` and fail fast on invalid parameter
  combinations.
- Benchmark payload is deterministic, so repeated runs compare the same input shape.