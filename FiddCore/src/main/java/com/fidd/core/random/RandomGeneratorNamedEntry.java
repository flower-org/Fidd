package com.fidd.core.random;

import com.fidd.core.NamedEntry;
import java.util.random.RandomGenerator;

public interface RandomGeneratorNamedEntry extends NamedEntry {
    RandomGenerator generator();
}
