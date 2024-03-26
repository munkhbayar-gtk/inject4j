package inject4j.context;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PropertySourceAppArgsLoader implements PropertySource{
    private final String[] args;
    PropertySourceAppArgsLoader(String ... args) {
        this.args = args;
    }
    @Override
    public Map<String, String> load() throws IOException {
        return Arrays.stream(args)
                .filter((arg)-> arg.startsWith("--"))
                .map((arg)-> arg.split("="))
                .collect(
                        Collectors
                                .toMap(
                                        (arg -> arg[0]),
                                        (arg-> arg.length > 1 ? arg[1] : null)
                                )
                );
    }

    private String arg(String[] arg, int index) {
        return arg.length > index ? arg[index] : null;
    }
}
