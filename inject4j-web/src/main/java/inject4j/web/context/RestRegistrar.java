package inject4j.web.context;

import java.lang.annotation.Annotation;
import java.util.function.Function;

public interface RestRegistrar {
    void register(Annotation mapping, String uri, Function<?, ?> execution);
}
