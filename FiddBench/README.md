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
gradle :FiddBench:jmhClasses
```

Run all benchmarks:

```bash
gradle :FiddBench:jmh
```

Run only encryption benchmarks:

```bash
gradle :FiddBench:jmhEncryption
```

This task runs:

- `EncryptionAlgorithmBenchmark`
- `EncryptionAlgorithmThroughputBenchmark`

Run only random-access benchmarks:

```bash
gradle :FiddBench:jmhRandomAccess
```

This task runs:

- `RandomAccessEncryptionAlgorithmBenchmark`
- `RandomAccessEncryptionAlgorithmThroughputBenchmark`

## Result files

JMH results are written as JSON because `FiddBench/build.gradle` sets:

```gradle
jmh {
    resultFormat = 'JSON'
}
```

Main task output:

- `FiddBench/build/results/jmh/results.json`

Subset task outputs:

- `FiddBench/build/results/jmh/jmhEncryption.json`
- `FiddBench/build/results/jmh/jmhEncryption.txt`
- `FiddBench/build/results/jmh/jmhRandomAccess.json`
- `FiddBench/build/results/jmh/jmhRandomAccess.txt`

## Notes

- Stream benchmarks write into a lightweight checksum sink and consume the checksum through JMH
  `Blackhole`. This keeps output bytes observable without accumulating the full output in memory.
- Random-access benchmarks validate `offset/length` combinations during `@Setup` and fail fast on invalid parameter
  combinations.
- Benchmark payload is deterministic, so repeated runs compare the same input shape.
