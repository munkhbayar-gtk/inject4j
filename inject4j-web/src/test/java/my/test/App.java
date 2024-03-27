package my.test;


import inject4j.core.annotation.Inject4jApp;
import inject4j.core.context.Inject4j;

@Inject4jApp()
public class App {
    public static void main(String[] args) {
        Inject4j.run(App.class, args);
    }


}
