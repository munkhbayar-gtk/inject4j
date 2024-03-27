package inject4j.core.context;

import inject4j.core.annotation.Inject4jApp;
import inject4j.core.utils.Inject4jUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Inject4j {

    private AnnotationAppContext appContext;
    private Inject4j() {
    }
    private Inject4j start(Class<?> entryPointClass, String ... args) {
        Environment env = createEnvironment(args);
        String[] basePackages = basePackages(entryPointClass);
        this.appContext = AnnotationAppContext.create(basePackages, env);
        this.appContext.start();
        return this;
    }
    public static Inject4j run(Class<?> entryPointClass, String ... args){
        Inject4j app = new Inject4j();
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(app::stop, "SHUTDOWN"));
        return app.start(entryPointClass, args);
    }

    public void stop() {
        appContext.close();
    }

    private String[] basePackages(Class<?> entryPointClass) {
        Inject4jApp app = entryPointClass.getAnnotation(Inject4jApp.class);
        if(app != null) {
            String [] basePackages = app.basePackages();
            if(basePackages != null) {
                List<String> tmpVar = Arrays
                        .stream(basePackages)
                        .filter(pkg-> pkg != null && pkg.trim().length() > 0)
                        .map(String::trim).toList();
                if(!tmpVar.isEmpty()) {
                    return tmpVar.toArray(new String[]{});
                }
            }
        }
        String basePackage = entryPointClass.getPackageName();
        return new String[]{ basePackage };
    }
    private Environment createEnvironment(String ... args) {
        PropertySource[] propertySources = {
                new PropertySourceResourceLoader("classpath:/bootstrap.properties"),
                new PropertySourceResourceLoader("classpath:/application.properties"),
                new PropertySourceEnvLoader(),
                new PropertySourceAppArgsLoader(args)
        };
        Map<String, String> properties = load(propertySources);
        String activeProfiles = properties.getOrDefault("inject4j.active.profiles", "default");
        return new Environment(
                Arrays.stream(activeProfiles.split(",")).collect(Collectors.toSet()),
                properties
        );
    }

    private Map<String, String> load(PropertySource [] propertySources) {
        Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < propertySources.length; i++) {
            PropertySource propertySource = propertySources[i];
            try{
                Map<String, String> loadedProperties = propertySource.load();
                properties.putAll(loadedProperties);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        Map<String, String> resolves = new HashMap<>(properties);
        resolve(resolves);
        return resolves;
    }
    private void resolve(Map<String, String> source) {
        PropertyResolver resolver = new PropertyResolver();
        resolver.resolve(source);
    }
}
