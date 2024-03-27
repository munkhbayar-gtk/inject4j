package inject4j.core.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Profile {
    /**
     * OR matching,
     * //@Profile("dev", "test", "qa"), checks if any of them is activated in 'inject4j.active.profiles' property,
     * @return
     */
    String[] value() default "";
}
