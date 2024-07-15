package com.nageoffer.shortlink;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Unit test for simple App.
 */
public class AppTest {
    public static void main(String[] args) {
        ThreadLocalRandom current = ThreadLocalRandom.current();
        int i = current.nextInt(60);

    }
}
