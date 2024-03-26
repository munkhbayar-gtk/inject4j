package inject4j.context;

import inject4j.annotation.Inject;
import inject4j.annotation.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class MethodBeanFactory implements BeanFactory{
    private final Object obj;
    private final Method method;
    private final InstanceProvider instanceProvider;

    public MethodBeanFactory(Object obj, Method method, InstanceProvider instanceProvider) {
        this.obj = obj;
        this.method = method;
        this.instanceProvider = instanceProvider;
    }

    @Override
    public Object create() throws RuntimeException{
        try{
            Parameter[] parameters = method.getParameters();
            int parameterCount = method.getParameterCount();
            if(parameterCount == 0) {
                return method.invoke(obj);
            }
            Object [] args = new Object[parameterCount];
            for(int i = 0 ; i< parameterCount ; i ++) {
                Parameter parameter = parameters[i];
                Annotation[] annotations = parameter.getAnnotations();
                InstanceRef ref = instanceProvider.getRef(annotations, parameter.getType());
                if(!ref.provided()) {
                    ref = instanceProvider.getRef(parameter.getType());
                }
                if(!ref.provided()) {
                    String msg = String.format("NoProvider found for: %s.%s, param-index: %d, param-type: %s, param-name: %s",
                            obj.getClass().getCanonicalName(),method.getName(), i, parameter.getType(), parameter.getName());
                    throw new IllegalArgumentException(msg);
                }
                args[i] = ref.instance();
            }
            return method.invoke(obj, args);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
