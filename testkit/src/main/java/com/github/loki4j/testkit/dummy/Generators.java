package com.github.loki4j.testkit.dummy;

import java.util.concurrent.ThreadLocalRandom;

public class Generators {
    
    public static String genMessage(int maxWords) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        StringBuilder msg = new StringBuilder();
        int words = rnd.nextInt(1, maxWords);
        for (int i = 0; i < words; i++) {
            int letters = rnd.nextInt(1, 20);
            for (int j = 0; j < letters; j++) {
                msg.append(rnd.nextFloat() < 0.1
                    ? (char)('A' + rnd.nextInt('Z' - 'A'))
                    : (char)('a' + rnd.nextInt('z' - 'a')));
            }
            msg.append(rnd.nextFloat() < 0.05
                ? '-'
                : ' ');
        }
        return msg.toString();
    }

}
