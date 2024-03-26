package inject4j.context;

public record InstanceRef(
        boolean provided,
        Object instance
) {
}
