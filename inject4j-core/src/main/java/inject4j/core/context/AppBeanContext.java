package inject4j.core.context;

import java.lang.annotation.Annotation;
import java.util.List;

public interface AppBeanContext {

    Object createInstnce(Class<?> beanClass, String beanNames);

    boolean isRestricted(Class<?> beanClass);

    List<Class<?>> findClassesByAnnotation(Class<? extends Annotation> annotation);
}
