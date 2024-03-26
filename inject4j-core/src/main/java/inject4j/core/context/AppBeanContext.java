package inject4j.core.context;

public interface AppBeanContext {

    Object createInstnce(Class<?> beanClass);

    boolean isRestricted(Class<?> beanClass);

}
