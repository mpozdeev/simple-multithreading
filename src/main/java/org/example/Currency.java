package org.example;

import java.util.Random;

public enum Currency {
    USD, EUR, RUB;

    private static final Random random = new Random();
    private static final Currency[] VALUES = values();

    public static Currency random() {
        return VALUES[random.nextInt(VALUES.length)];
    }
}
