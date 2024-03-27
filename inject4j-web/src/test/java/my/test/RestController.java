package my.test;

import inject4j.web.annotation.Rest;

@Rest("rest")
public class RestController {
    public RestController() {
        System.out.println("me created");
    }
}
