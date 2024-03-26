package inject4j.core.context;

import java.util.Map;
import java.util.Set;

public class Environment {
    public final Set<String> activeProfiles;
    private final Map<String, String> properties;
    Environment(Set<String> activeProfiles, Map<String, String> properties) {
        this.activeProfiles = activeProfiles;
        this.properties = properties;
    }

    public final String getProperty(String prop) {
        return properties.get(prop);
    }
    public final String getProperty(String prop, String defaultValue) {
        return properties.getOrDefault(prop, defaultValue);
    };
    public final boolean hasProperty(String prop) {
        return properties.containsKey(prop);
    }
}
