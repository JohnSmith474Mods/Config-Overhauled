package johnsmith.configoverhauled.api.registry;

import johnsmith.configoverhauled.api.data.Color;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.factory.PropertyFactory;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the mapping of native Java types to their corresponding dynamic property factories.
 * Required for translating authoritative server definitions or external datapack objects into functional property instances.
 */
public final class DynamicPropertyTypeRegistry {
    private static final Map<Class<?>, PropertyFactory> REGISTRY = new ConcurrentHashMap<>();

    static {
        register(Float.class, (name, group, def, min, max) -> new PropertyImpl.Float(name, group, ConfigScope.LEVEL, null, ((Number) def).floatValue(), ((Number) min).floatValue(), ((Number) max).floatValue(), true));
        register(Integer.class, (name, group, def, min, max) -> new PropertyImpl.Integer(name, group, ConfigScope.LEVEL, null, ((Number) def).intValue(), ((Number) min).intValue(), ((Number) max).intValue(), true));
        register(Double.class, (name, group, def, min, max) -> new PropertyImpl.Double(name, group, ConfigScope.LEVEL, null, ((Number) def).doubleValue(), ((Number) min).doubleValue(), ((Number) max).doubleValue(), true));
        register(Long.class, (name, group, def, min, max) -> new PropertyImpl.Long(name, group, ConfigScope.LEVEL, null, ((Number) def).longValue(), ((Number) min).longValue(), ((Number) max).longValue(), true));
        register(Color.class, (name, group, def, min, max) -> new PropertyImpl.Color(name, group, ConfigScope.LEVEL, null, ((Number) def).intValue(), true));
        register(Boolean.class, (name, group, def, min, max) -> new PropertyImpl.Boolean(name, group, ConfigScope.LEVEL, null, (Boolean) def, true));
        register(String.class, (name, group, def, min, max) -> new PropertyImpl.String(name, group, ConfigScope.LEVEL, null, (String) def, true));
    }

    /**
     * Binds a custom factory implementation to a target data type, permitting framework expansion.
     *
     * @param propertyType The base class type managed by the property.
     * @param factory      The functional instantiation logic for the target type.
     */
    public static void register(Class<?> propertyType, PropertyFactory factory) {
        REGISTRY.putIfAbsent(propertyType, factory);
    }

    /**
     * Resolves the factory instance bound to the designated data type.
     *
     * @param propertyType The requested class type.
     * @return The corresponding factory implementation.
     * @throws IllegalStateException If the requested type lacks a registered factory.
     */
    public static PropertyFactory get(Class<?> propertyType) {
        PropertyFactory factory = REGISTRY.get(propertyType);
        if (factory == null) {
            throw new IllegalStateException("Unregistered dynamic property type: " + propertyType);
        }
        return factory;
    }
}