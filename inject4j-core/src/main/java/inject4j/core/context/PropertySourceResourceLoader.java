package inject4j.core.context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Objects;

class PropertySourceResourceLoader extends PropertySourceInputStream{
    PropertySourceResourceLoader(String path) {
        super(input(path));
    }

    private static InputStream input(String path){
        URI uri = URI.create(path);
        String proto = uri.getScheme();
        if(Objects.equals(proto, "classpath")) {
            return classpath(path.substring("classpath:".length()));
        }else if(Objects.equals(proto, "file")) {
            return file(path.substring("file:".length()));
        }else if(Objects.equals(proto, "http")) {
            return http(uri);
        }else if(Objects.equals(proto,"https")) {
            return http(uri);
        }
        throw new IllegalArgumentException(String.format("UNKNOWN Property Source for: %s", path));
    }

    private static InputStream classpath(String path) {
        return PropertySourceInputStream.class.getResourceAsStream(path);
    }
    private static InputStream file(String path) {
        try{
            return new FileInputStream(path);
        }catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static InputStream http(URI uri) {
        try{
            URLConnection con = uri.toURL().openConnection();
            return con.getInputStream();
        }catch (IOException e ) {
            throw new IllegalArgumentException(e);
        }
    }
}
