package my.test;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebTestCasesTest extends TestCase {

    public void testExtractVariables() {
        System.out.println(System.getProperties());
        String template = "/root/context/page/${id}/${name}/${parentId}-${childId}";
        String input = "/root/context/page/1/test/123-12";

        Map<String, String> values = extractValues(template, input);

        if (values.size() > 0) {
            System.out.println(values); // Output: {id=1, name=test}
        } else {
            System.out.println("Input string doesn't match the template");
        }
    }

    private static Map<String, String> extractValues(String template, String input) {
        Pattern pattern = Pattern.compile(template.replaceAll("\\$\\{([^}]+)\\}", "(?<$1>[^/]*)"));
        Matcher matcher = pattern.matcher(input);

        Map<String, String> extractedValues = new HashMap<>();
        if (matcher.matches()) {
            for (String groupName : matcher.namedGroups().keySet()) {
                extractedValues.put(groupName, matcher.group(groupName));
            }
        }

        return extractedValues;
    }
}
