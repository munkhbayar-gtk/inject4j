package inject4j.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ConditionOnProperty {
    /**
     * //@ConditionOnProperty({"my.prop1", "my.prop2"}) checks if all of them is defined in active profile's settings.
     * @return
     */
    String[] value() default "";
}
