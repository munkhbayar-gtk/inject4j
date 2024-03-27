package my.test;


import inject4j.core.annotation.Bean;
import inject4j.core.annotation.Context;

import java.util.Random;

@Context
public class Ctx {
    public Ctx() {
        System.out.println("xtc create");
    }

    @Bean()
    public Integer myInt() {
        return new Random().nextInt(1000);
    }
}
