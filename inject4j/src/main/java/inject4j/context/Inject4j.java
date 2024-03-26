package inject4j.context;

import javax.naming.spi.Resolver;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Inject4j {

    private AnnotationAppContext appContext;
    private final Class<?> [] contextClasses;
    public Inject4j(Class<?> ... contextClasses) {
        this.contextClasses = contextClasses;

    }
    public void start(String ... args){
        Environment env = createEnvironment(args);
        this.appContext = AnnotationAppContext.create(env, contextClasses);
    }

    public void stop() {
        appContext.close();
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
        for (PropertySource propertySource : propertySources) {
            try{
                Map<String, String> loadedProperties = propertySource.load();
                properties.putAll(loadedProperties);
            }catch (IOException e) {
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
