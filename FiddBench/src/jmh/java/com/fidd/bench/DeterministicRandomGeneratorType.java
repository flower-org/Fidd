package com.fidd.bench;

import com.fidd.core.random.RandomGeneratorType;
import java.util.Random;
import java.util.random.RandomGenerator;

final class DeterministicRandomGeneratorType implements RandomGeneratorType {
  private final Random random;

  DeterministicRandomGeneratorType(long seed) {
    this.random = new Random(seed);
  }

  @Override
  public RandomGenerator generator() {
    return random;
  }

  @Override
  public String name() {
    return "DeterministicRandom";
  }
}
