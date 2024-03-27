package inject4j.core.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Inject4jUtils {

    public static List<Class<?>> findClassesByAnnotation(String basePackage, Class<? extends Annotation> annotation) {

        try{
            List<Class<?>> classes = new ArrayList<>();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = basePackage.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            while(resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                classes.addAll(findClasses(resource.getPath(), basePackage, annotation));
            }
            //System.out.printf("base: %s, annotation: %s, found: %d", basePackage, annotation.getCanonicalName(), classes.size());
            return classes;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Class<?>> findClasses(String directoryPath, String packageName, Class<? extends Annotation> annotation) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if(files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file.getPath(), packageName + "." + file.getName(), annotation));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = Class.forName(className);
                    // Check if the class is not abstract or an inner class
                    if (!Modifier.isAbstract(clazz.getModifiers()) && !clazz.isMemberClass()) {
                        if (clazz.isAnnotationPresent(annotation)) {
                            classes.add(clazz);
                        }
                    }
                }
            }
        }
        return classes;
    }

    public static boolean isEmpty(String vl) {
        return vl == null || vl.trim().length() == 0;
    }
    public static String camel(Class<?> clzz) {
        String className = clzz.getName();
        StringBuilder sb = new StringBuilder();
        boolean nextCharToUpper = false;
        for (char c : className.toCharArray()) {
            if (c == '_') {
                nextCharToUpper = true;
            } else {
                sb.append(nextCharToUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                nextCharToUpper = false;
            }
        }
        return sb.toString();
    }
}
