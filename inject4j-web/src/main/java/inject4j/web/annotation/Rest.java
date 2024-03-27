package inject4j.web.annotation;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
public @interface Rest {
    String value() default "" ;

    String[] routes() default "";
}
