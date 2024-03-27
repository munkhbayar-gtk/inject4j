package inject4j.web.context;

import inject4j.core.context.AppBeanContext;
import inject4j.core.context.BeanFactory;
import inject4j.core.context.Environment;
import inject4j.web.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

class RestBeanFactory implements BeanFactory {
    private final Environment env;
    private final Class<?> restClass;
    private final AppBeanContext context;

    RestBeanFactory(Environment env, Class<?> restClass, AppBeanContext context) {
        this.env = env;
        this.restClass = restClass;
        this.context = context;
    }

    @Override
    public Object create() {
        Rest rest = restClass.getAnnotation(Rest.class);
        Object instance = context.createInstnce(restClass, rest.value()); // create it from contructor with injections;

        String [] routes = rest.routes();
        String contextPath = env.getProperty("inject4j.web.context.path", "/");
        List<Method> methods = Arrays.stream(restClass.getDeclaredMethods()).filter(this::isRestMethod).toList();
        methods.forEach(m-> bindMethod(instance, contextPath, routes, m));
        /*
         */
        return instance;
    }

    private void bindMethod(Object instance, String contextPath, String[] routes, Method method) {
        HMethod service = service(method);
        for (String route : routes) {
            for (String serviceMethodPath : service.paths) {
                String path = path(contextPath, route, serviceMethodPath);
                bind(instance, method, service, path);
            }
        }
    }

    private HMethod service(Method m) {
        Option opt = m.getAnnotation(Option.class);
        if(opt != null) {
            return HMethod.of(opt,opt.value());// opt.value();
        }
        Delete del = m.getAnnotation(Delete.class);
        if(del != null) {
            return HMethod.of(del,del.value());
        }
        Get get = m.getAnnotation(Get.class);
        if(get != null) {
            return HMethod.of(get,get.value());
        }
        Put put = m.getAnnotation(Put.class);
        if(put != null) {
            return HMethod.of(put,put.value());
        }
        Post post = m.getAnnotation(Post.class);
        if(post != null) {
            return HMethod.of(post, post.value());
        }
        throw new IllegalArgumentException(m.getName() + " has no Rest Mapping annotations such as Get, Post, Put, Delete, Option");
    }

    private String path(String ... sections) {
        String[] clears = new String[sections.length];
        for (int i = 0; i < sections.length; i++) {
            String cleared = clearPath(sections[i]);
            clears[i] = cleared;
        }
        return String.join("/", clears);
    }
    private String clearPath(String section) {
        StringBuilder sb = new StringBuilder();
        boolean ignore = false;
        for(int i = 0 ; i < section.length() ; i ++) {
            char c = section.charAt(i);
            if(c == '/') {
                if(ignore) {
                    continue;
                }
                ignore = true;
            }else{
                ignore = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }
    private void bind(Object instance, Method m, HMethod service, String urlPath) {
        Parameter[] params = m.getParameters();
        int paramCount = m.getParameterCount();
        List<Supplier<?>> paramValueSupplier =new ArrayList<>(paramCount);

        for(int i = 0 ; i < paramCount ; i ++) {
            Parameter parameter = params[i];
            Class<?> type = parameter.getType();

        }
        Function<?, ?> link = new Function<Object, Object>() {
            @Override
            public Object apply(Object o) {
                Object[] paramValues = new Object[paramCount];
                for(int i = 0 ; i < paramCount ; i ++) {
                    paramValues[i] = paramValueSupplier.get(i).get();
                }
                try{
                    return m.invoke(instance, paramValues);
                }catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
        };
        RestRegistrar registrar = null;
        registrar.register(service.method, urlPath, link);
    }
    private boolean isRestMethod(Method method) {
        Annotation[] annotations = method.getAnnotations();
        return Arrays.stream(annotations).anyMatch(a -> REST_METHODS.contains(a.annotationType()));
    }

    private static final Set<Class<?>> REST_METHODS = Set.of(
            Get.class,
            Option.class,
            Delete.class,

            Post.class,
            Put.class
    );
    private record HMethod(Annotation method, String[] paths) {
        static HMethod of(Annotation method, String[] paths) {
            return new HMethod(method,paths);
        }
    }
}
