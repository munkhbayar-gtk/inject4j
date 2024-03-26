package inject4j.core.context;

public class BeanRef {
    private boolean created;

    private final Class<?> beanType;
    private Object bean;
    private final String name;

    private final BeanFactory factory;
    private final OnBeanCreation onBeanCreation;
    BeanRef(String name, Class<?> beanType, BeanFactory factory, OnBeanCreation onBeanCreation) {
        this.beanType = beanType;
        this.name = name;
        this.factory = factory;
        this.onBeanCreation = onBeanCreation;
    }

    boolean isAssignableFrom(Class<?> type) {
        return beanType == type || beanType.isAssignableFrom(type);
    }

    String getName() {
        return name;
    }
    Object getBean() {
        if(created) {
            return bean;
        }
        onBeanCreation.onPre();
        bean = factory.create();
        created = true;
        onBeanCreation.onPost();
        return bean;
    }
    interface OnBeanCreation {
        void onPre();
        void onPost();
    }
}
