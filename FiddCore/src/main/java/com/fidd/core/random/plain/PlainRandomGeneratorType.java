package com.fidd.core.random.plain;

import com.fidd.core.random.RandomGeneratorType;

import java.util.Random;
import java.util.random.RandomGenerator;

public class PlainRandomGeneratorType implements RandomGeneratorType {
    protected Random random = new Random();

    @Override
    public RandomGenerator generator() {
        return random;
    }

    @Override
    public String name() {
        return "Random";
    }
}
