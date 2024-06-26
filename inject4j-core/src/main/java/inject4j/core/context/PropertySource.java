package inject4j.core.context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

interface PropertySource {
    Map<String, String> load() throws IOException;
}
