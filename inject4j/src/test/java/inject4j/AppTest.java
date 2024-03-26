package inject4j;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }

    public void testSplit() {
        String str = "testvalueabc${variable.name}someothervalue${prop.key}";
        String[] split = str.split("(?=\\$\\{|\\})|(?<=\\$\\{|\\})");

        for (String s : split) {
            System.out.println(s);
        }
    }
    public void testEscapeOr() {
        String variable = "\\${test}";
        Pattern PATTERN = Pattern.compile("\\$\\{(.*?)\\}|(\\\\\\\\?\\$\\{.*?\\})");
        Matcher matcher = PATTERN.matcher(variable);
        while(matcher.find()) {
            String group = matcher.group(0);
            System.out.println(group);
        }
        System.out.println(matcher.matches());
    }
    public void testEscape() {
        String variable = "\\${test}";
        Pattern ESCAPED_VARIABLE_PATTERN = Pattern.compile("\\\\\\\\?\\$\\{(.*?)\\}");
        Matcher matcher = ESCAPED_VARIABLE_PATTERN.matcher(variable);
        System.out.println(matcher.matches());
    }
    public void testResolver() {
        Resolver resolver = new Resolver();
        Map<String, String> src = new HashMap<>(Map.of(
                "test1", "test1",
                "test2", "test2-${test1} test ${test1}",
                "test3", "test3-${test1}-${test5}",
                "test4", "test4-${test3}",
                "test5", "test5-\\${test4}",
                "test6", "test6-${test2}-${test1}-${test3}-${test4} values"
        ));
        System.out.println(src);
        resolver.resolve(src);
        System.out.println(src);
    }
}
