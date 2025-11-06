package com.fidd.core.random.secure;

import com.fidd.core.random.RandomGeneratorType;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

public class SecureRandomGeneratorType implements RandomGeneratorType {
    protected SecureRandom random = new SecureRandom();

    @Override
    public RandomGenerator generator() {
        return random;
    }

    @Override
    public String name() {
        return "SecureRandom";
    }
}
