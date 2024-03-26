package inject4j.core.context;

import java.io.IOException;
import java.util.Map;

public class PropertySourceEnvLoader implements PropertySource{
    @Override
    public Map<String, String> load() throws IOException {
        return System.getenv();
    }
}
