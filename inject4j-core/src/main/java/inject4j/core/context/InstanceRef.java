package inject4j.core.context;

public record InstanceRef(
        boolean provided,
        Object instance
) {
}
