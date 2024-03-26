package inject4j.core.context;

import java.lang.annotation.Annotation;

public interface InstanceProvider {
    InstanceRef getRef(Annotation[] annotation, Class<?> type);

    InstanceRef getRef(Class<?> type);
}
