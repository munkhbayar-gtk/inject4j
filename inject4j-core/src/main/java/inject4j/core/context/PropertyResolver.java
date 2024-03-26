package inject4j.core.context;

import java.util.*;

class PropertyResolver {
    void resolve(Map<String, String> src) {
        Set<String> keys = new HashSet<>(src.keySet());
        keys.forEach(key -> {
            resolve(key,src,new LinkedHashSet<>());
        });
    }

    private void resolve(String k, Map<String, String> src, Set<String> visits) {
        if(visits.contains(k)) {
            throw new IllegalStateException(String.format("loop: [ %s ]", visits));
        }
        visits.add(k);
        String v = src.get(k);
        var parts = parts(v);
        StringBuilder sb = new StringBuilder();
        parts.forEach((p -> {
            if(p.var) {
                resolve(p.value, src, visits);
                String vl = src.get(p.value);
                sb.append(vl);
            }else{
                sb.append(p.value);
            }
        }));
        src.put(k,sb.toString());
        String finalValue = src.get(k);
        src.put(k,finalValue);
        visits.remove(k);
    }
    private Character charAt(String v , int i) {
        return -1 < i && i < v.length() ? v.charAt(i) : null;
    }
    private boolean isEscape(Character c) {
        return c != null && c == '\\';
    }

    private List<Part> parts(String v) {
        int lastIndex = 0;
        int startIndex = 0;
        boolean done;
        List<Part> parts = new LinkedList<>();
        do {
            int varIndex = v.indexOf("${", lastIndex);
            if(varIndex > -1) {
                Character prev = charAt(v, varIndex - 1);
                if(isEscape(prev)) {
                    // continue
                    lastIndex = varIndex + 2;
                }else{
                    String preceeding = v.substring(startIndex, varIndex);
                    parts.add(new Part(false, preceeding));
                    startIndex = varIndex + 2;
                    int closeIndex = v.indexOf("}", varIndex);
                    if(closeIndex < 0) {
                        throw new IllegalArgumentException("unclosed variable declaration at: " + varIndex);
                    }

                    String subKey = v.substring(startIndex, closeIndex);
                    parts.add(new Part(true, subKey));

                    lastIndex = closeIndex + 1;
                    startIndex = lastIndex;
                }
                done = lastIndex >= v.length();
            }else{
                var part = new Part(false, v.substring(startIndex));
                parts.add(part);
                done = true;
            }
        }while(!done);
        return parts;//.toArray(new Part[]{});
    }

    private record Part(boolean var, String value) {

        @Override
        public String toString() {
            return "Part{" +
                    "var=" + var +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
