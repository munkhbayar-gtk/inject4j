package inject4j.context;

import java.lang.annotation.Annotation;

public interface InstanceProvider {
    InstanceRef getRef(Annotation[] annotation, Class<?> type);

    InstanceRef getRef(Class<?> type);
}
