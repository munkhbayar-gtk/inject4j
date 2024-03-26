package inject4j.web.context;

import inject4j.core.context.*;
import inject4j.web.annotation.Rest;

import java.util.List;
import java.util.Objects;

public class WebAppBeansDefLoaderImpl implements BeanFactoriesLoader {

    private BeanFactory createBeanFactory(Class<?> restClass, Environment env, AppBeanContext context) {
        if(context.isRestricted(restClass)) {
            return null; //
        }
        Rest rest = restClass.getAnnotation(Rest.class);
        if(rest != null) {
            return new RestBeanFactory(env, restClass, context);
        }
        return null;
    }
    private List<Class<?>> rests() {
        // load rest classes from app basePackage.

        return List.of();
    }


    @Override
    public List<BeanFactory> getBeanFactories(Environment env, AppBeanContext context) {
        var restClasses = rests();
        return restClasses.stream()
                .map(restClass -> createBeanFactory(restClass, env, context))
                .filter(Objects::nonNull)
                .toList();
    }
}
