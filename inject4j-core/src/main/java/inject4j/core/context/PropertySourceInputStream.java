package inject4j.core.context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class PropertySourceInputStream implements  PropertySource{
    private final InputStream input;
    PropertySourceInputStream(InputStream input) {
        this.input = input;
    }
    @Override
    public Map<String, String> load() throws IOException {
        Map<String, String> ret = new HashMap<>();
        try{
            Properties properties = new Properties();
            properties.load(input);
            properties.forEach((k,v)-> ret.put((String)k,(String)v));
        }catch (IOException e) {
            throw e;
        }finally {
            if(input != null)
                input.close();
        }
        return ret;
    }
}
