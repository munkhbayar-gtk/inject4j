package inject4j.core.context;

import inject4j.core.annotation.*;
import inject4j.core.utils.Inject4jUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class AnnotationAppContext implements AppBeanContext{

    private final Map<String, BeanRef> beans = new HashMap<>();
    private final Set<Class<?>> contextClasses;
    private final Environment environment;

    private final String[] basePackages;
    static AnnotationAppContext create(String[] basePackages, Environment environment ) {
        return new AnnotationAppContext(basePackages, environment);
    }
    private AnnotationAppContext(String[] basePackages, Environment environment) {
        this.environment = environment;
        this.basePackages = basePackages;
        this.contextClasses = findContextClasses();
    }

    private Set<Class<?>> findContextClasses() {
        return Arrays.stream(basePackages)
                .map(basePackage -> Inject4jUtils.findClassesByAnnotation(basePackage, Context.class))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    void start(){
        contextClasses.forEach(this::createContext);
        beans.forEach((k,v)->{
            createBean(v);
        });
    }
    private void createBean(BeanRef ref) {
        ref.getBean();
    }
    private void createContext(Class<?> contextClass) throws RuntimeException{
        Object context = null;
        try{
            Constructor<?> constructor = contextClass.getConstructor();
            context = constructor.newInstance();
        }catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        final Object appContext = context;
        List<BeanRef> refs = Arrays.stream(contextClass.getDeclaredMethods()).filter((method->{
            Bean bean = method.getAnnotation(Bean.class);
            if(bean != null) {
                int modifiers = method.getModifiers();
                boolean legit = Modifier.isPublic(modifiers) && !Modifier.isInterface(modifiers);
                if(!legit) {
                    throw new IllegalArgumentException(String.format("@Bean method must be public and non-static: %s%s", contextClass.getCanonicalName(), method.getName()));
                }

                Profile profile = method.getAnnotation(Profile.class);
                if(profile != null) {
                    String [] profiles = profile.value();
                    Set<String> activeProfiles = environment.activeProfiles;
                    boolean activated = Arrays.stream(profiles).anyMatch(activeProfiles::contains);
                    if(!activated) {
                        return false;
                    }
                }

                ConditionOnProperty condition = method.getAnnotation(ConditionOnProperty.class);
                if(condition != null) {
                    String [] properties = condition.value();
                    return isAllOn(properties);
                }
            }
            return true;
        })).map((method -> create(contextClass, appContext, method))).toList();
        refs.forEach((ref-> beans.put(ref.getName(), ref)));
    }

    private boolean isAllOn(String[] properties) {
        return Arrays.stream(properties).allMatch(environment::hasProperty);//filter(environment::hasProperty).count() == properties.length;
    }
    private boolean isAnyOn(String[] properties) {
        return Arrays.stream(properties).anyMatch(environment::hasProperty);// properties.length;
    }

    private String[] keyAndDefValue(Value value) {
        String propRef = value.value();
        for(PropPatternMatcher m : MATCHERS) {
            String[] values = m.matches(propRef);
            if(values != null) {
                return values;
            }
        }
        throw new IllegalArgumentException(String.format("Invalid Prop Access: %s", propRef));
    }
    private InstanceRef getPropValue(Value value, Class<?> type) {
        // ${*}
        // ${*:*}
        String propRef = value.value();
        for(PropPatternMatcher m : MATCHERS) {
            String[] values = m.matches(propRef);
            if(values != null) {
                String prop = values[0];
                String def = values[1];
                if(!environment.hasProperty(prop)) {
                    return new InstanceRef(false, null);
                }
                String propValue = environment.getProperty(prop,def);
                return new InstanceRef(true, stringTo(propValue, type));
            }
        }
        throw new IllegalArgumentException(String.format("Invalid Prop Access: %s", propRef));
    }

    private Object stringTo(String str, Class<?> to) {
        IStringTo<?> convertor = STRINGIFIERS.get(to);
        if(convertor == null) {
            throw new IllegalArgumentException(String.format("No String Convertor found for type: %s", to));
        }
        return convertor.to(str);
    }

    private Set<String> track = new LinkedHashSet<>();
    private String loop(String name) {
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        for(String bn : track) {
            if(found) {
                sb.append(name).append("->");
            }else{
                found = Objects.equals(bn, name);
            }
        }
        int len = sb.length();
        sb.setLength(len - 2);
        return sb.toString();
    }

    private String beanName(Supplier<String> err, String ... names) {
        return Arrays.stream(names)
                .filter(nm -> nm != null && nm.trim().length() > 0)
                .map(String::trim)
                .findFirst()
                .orElseThrow((Supplier<RuntimeException>) () -> new IllegalArgumentException(err.get()));
    }

    private InstanceRef getBean(Class<?> contextClass, Object context, Method method, Inject inject, Class<?> type) {
        /*String name = inject.name();
        if(name == null || name.length() == 0 || name.trim().length() == 0) {
            throw new IllegalArgumentException(String.format("%s.%s @Inject must have name defined", contextClass.getCanonicalName(), method.getName()));
        }*/
        String name = beanName(
                ()-> String.format("%s.%s @Inject must have name defined", contextClass.getCanonicalName(), method.getName()),
                inject.name(),
                method.getName());
        if(!beans.containsKey(name)) return new InstanceRef(false, null);
        if(track.contains(name)) {
            String loop = loop(name);
            throw new IllegalArgumentException(String.format("Circular Declaration Occurred: %s", loop));
        }
        BeanRef ref = beans.get(name);
        return new InstanceRef(true, ref.getBean());
    }
    private InstanceRef getBean(Class<?> type) {
        List<BeanRef> refs = beans.values().stream().filter((ref -> ref.isAssignableFrom(type))).toList();
        if(refs.size() == 0) {
            throw new IllegalArgumentException(String.format("No bean found: %s", type.getCanonicalName()));
        }
        if(refs.size() > 1) {
            var beans = refs.stream().map((BeanRef::getName)).toList();
            throw new IllegalArgumentException(String.format("Multiple bean found: %s, beans: %s", type.getCanonicalName(), beans));
        }
        BeanRef ref = refs.get(0);
        return new InstanceRef(true, ref.getBean());
    }
    private BeanRef create(Class<?> contextClass, Object context, Method method) {
        MethodBeanFactory beanFactory = new MethodBeanFactory(context, method, new InstanceProvider() {
            @Override
            public InstanceRef getRef(Annotation[] annotations, Class<?> type) {
                List<Annotation> sorted = sort(annotations);
                for(Annotation a : sorted) {
                    if(a instanceof Value v) {
                        return getPropValue(v, type);
                    }
                    if(a instanceof Inject inject) {
                        return getBean(contextClass, context, method, inject, type);
                    }
                }
                //List<Annotation> sorted = sort(annotations);
                return getBean(type);
            }

            @Override
            public InstanceRef getRef(Class<?> type) {
                return null;
            }
        });
        Bean bean = method.getAnnotation(Bean.class);
        String name = first(bean.value(), method.getName());
        Class<?> returnType = method.getReturnType();
        return new BeanRef(name, returnType, beanFactory, getOnBeanCreation(name));
    }
    public final void close(){
        beans.clear();
    }

    private BeanRef.OnBeanCreation getOnBeanCreation(String name) {
        return new BeanRef.OnBeanCreation() {
            @Override
            public void onPre() {
                track.add(name);
            }

            @Override
            public void onPost() {
                track.remove(name);
            }
        };
    }

    public <T> T getBean(String name) {
        BeanRef ref = beans.get(name);
        return (T) ref.getBean();
    }

    public <T> List<T> getBeans(Class<T> clazz) {
        List<T> ret = new ArrayList<T>(beans.size());
        beans.forEach((k,ref)->{
            var bean = ref.getBean();
            if(bean != null && bean.getClass().isAssignableFrom(clazz)) {
                ret.add((T)bean);
            }
        });

        return ret.isEmpty() ? null : ret;
    }

    private static String first(String ... values) {
        for(String vl : values) {
            if(vl != null && vl.length() > 0) {
                return vl;
            }
        }
        return null;
    }

    private List<Annotation> sort(Annotation[] annotations) {
        var pririoty = getPrioritizedAnnotations();
        var valToIndex = IntStream.range(0,pririoty.size())
                .boxed().collect(Collectors.toMap(
                        pririoty::get,
                        (index)->index
                ));
        return Arrays.stream(annotations).toList().stream().sorted((a, b) -> {
            int aa = valToIndex.get(a.annotationType());
            int bb = valToIndex.get(b.annotationType());
            return Integer.compare(aa,bb);
        }).toList();
    }
    private List<Class<?>> getPrioritizedAnnotations() {
        //TODO: load from auto-configured contexts
        return List.of(
                Value.class,
                Inject.class
        );
    }

    private final static PropPatternMatcher[] MATCHERS = {
            new PropPatternMatcher("\\$\\{(.*):(.*?)\\}"),
            new PropPatternMatcher("\\$\\{(.*?)\\}"),
    };

    @Override
    public Object createInstnce(Class<?> beanClass, String beanName) {
        BeanRef beanRef = createAndGetBeanRef(beanClass, beanName);
        return beanRef.getBean();
    }

    private BeanRef createAndGetBeanRef(Class<?> beanClass, String beanName) {
        final String _beanName = Inject4jUtils.isEmpty(beanName) ? Inject4jUtils.camel(beanClass) : beanName.trim();
        if(beans.containsKey(beanName)){
            BeanRef ref = beans.get(beanName);
            String msg = String.format("Duplicated Beans: %s, [ %s ]", beanName,
                    String.join(",",
                            Set.of(ref.beanType, beanClass).stream().map(Class::getCanonicalName).toList()
                    ));
            throw new IllegalStateException(msg);
        }
        List<Constructor<?>> constructors = Arrays.stream(beanClass.getDeclaredConstructors()).toList();
        if(constructors.size() > 1) {
            throw new IllegalStateException(String.format("multiple constructors defined in the class: %s", beanClass.getCanonicalName()));
        }
        Constructor<?> constructor = constructors.get(0);
        int paramCount = constructor.getParameterCount();
        Parameter[] params = constructor.getParameters();
        BeanRef[] paramRef = new BeanRef[paramCount];
        for (int i = 0; i < paramCount; i++) {
            Parameter param = params[i];
            paramRef[i] = getBeanRef(param);
        }
        BeanRef beanRef = new BeanRef(beanName, beanClass, () -> {
            try {
                Object[] paramValues = new Object[paramCount];
                for (int i = 0; i < paramCount; i++) {
                    paramValues[i] = paramRef[i].getBean();
                }
                return constructor.newInstance(paramValues);
            } catch (Exception e) {
                throw new IllegalStateException(
                        String.format("Failed to create a bean: %s, %s", _beanName, beanClass)
                );
            }
        }, getOnBeanCreation(_beanName));
        beans.put(beanRef.getName(), beanRef);
        return beanRef;
    }
    private BeanRef getBeanRef(Parameter parameter) {
        Value value = parameter.getAnnotation(Value.class);
        if(value != null) {
            String keyAndDefValue[] = keyAndDefValue(value);
            //String vl = environment.getProperty(keyAndDefValue[0], keyAndDefValue[1]);
            InstanceRef ref = getPropValue(value, parameter.getType());
            if(!ref.provided()) {
                throw new IllegalArgumentException(
                        String.format("Invalid Property key found: %s", keyAndDefValue[0])
                );
            }
            return new BeanRef(keyAndDefValue[0], parameter.getType(), ref::instance, getOnBeanCreation(keyAndDefValue[0]));
        }
        Inject inject = parameter.getAnnotation(Inject.class);
        String beanName = beanName(
                ()->
                        String.format("No bean found: %s", parameter.getType().getCanonicalName()),
                inject != null ? inject.name() : "",
                parameter.getName()
        );
        return Optional.ofNullable(
                beans.get(beanName)
        ).orElseThrow(()-> new NoSuchElementException(
                String.format("No bean found: %s, %s", beanName, parameter.getType().getCanonicalName())
        ));
    }

    @Override
    public boolean isRestricted(Class<?> beanClass) {
        Profile profile = beanClass.getAnnotation(Profile.class);
        if(profile != null) {
            String[] activatedProfiles = profile.value();
            Set<String> activeProfiles = environment.activeProfiles;
            boolean profileActivated = Arrays
                    .stream(activatedProfiles)
                    .anyMatch(activeProfiles::contains);
            if(!profileActivated) {
                return true;
            }
        }
        ConditionOnProperty condition = beanClass.getAnnotation(ConditionOnProperty.class);
        if(condition != null) {
            String[] propertiesRequested = condition.value();
            return Arrays
                    .stream(propertiesRequested)
                    .allMatch(environment::hasProperty);
        }
        return false;
    }

    @Override
    public List<Class<?>> findClassesByAnnotation(Class<? extends Annotation> annotation) {
        return Arrays.stream(basePackages)
                .map(basePackage -> Inject4jUtils.findClassesByAnnotation(basePackage, annotation))
                .flatMap(Collection::stream)
                .toList();
    }

    private static class PropPatternMatcher {
        final Pattern pattern;

        public PropPatternMatcher(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        String [] matches(String input) {
            Matcher matcher = pattern.matcher(input);
            if(!matcher.matches()){
                return null;
            }
            String[] ret = new String[matcher.groupCount()+1];
            for(int i = 0 ; i < ret.length ; i ++) {
                ret[i] = matcher.group(i + 1);
            }
            return ret;
        }
    }

    private interface IStringTo<T> {
        T to(String str);
    }
    private final Map<Class<?>, IStringTo<?>> STRINGIFIERS = new HashMap<>(){{
        put(int.class, (IStringTo<Integer>) str -> toNum(str).intValue());
        put(Integer.class, (IStringTo<Integer>) str -> toNum(str).intValue());
        put(BigInteger.class, (IStringTo<BigInteger>) str -> toNum(str).toBigInteger());


        put(short.class, (IStringTo<Short>) str -> toNum(str).shortValue());
        put(Short.class, (IStringTo<Short>) str -> toNum(str).shortValue());

        put(byte.class, (IStringTo<Short>) str -> toNum(str).shortValue());
        put(Byte.class, (IStringTo<Short>) str -> toNum(str).shortValue());

        put(double.class, (IStringTo<Double>) str -> toNum(str).doubleValue());
        put(Double.class, (IStringTo<Double>) str -> toNum(str).doubleValue());

        put(float.class, (IStringTo<Float>) str -> toNum(str).floatValue());
        put(Float.class, (IStringTo<Float>) str -> toNum(str).floatValue());

        put(long.class, (IStringTo<Long>) str -> toNum(str).longValue());
        put(Long.class, (IStringTo<Long>) str -> toNum(str).longValue());

        put(BigDecimal.class, (IStringTo<BigDecimal>) str -> toNum(str));
        put(Number.class, (IStringTo<Number>) str -> toNum(str));

        put(CharSequence.class, (IStringTo<Number>) str -> toNum(str));
        put(char[].class, (String::toCharArray));
        put(Character[].class, (String::toCharArray));
    }};

    private BigDecimal toNum(String str) {
        return new BigDecimal(str);
    }
}
