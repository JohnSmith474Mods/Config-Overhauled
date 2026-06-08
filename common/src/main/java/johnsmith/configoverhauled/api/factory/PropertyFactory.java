package johnsmith.configoverhauled.api.factory;

import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;

/**
 * Functional interface responsible for instantiating configuration properties at runtime.
 * Utilized by the dynamic registry to construct properties from deserialized network or disk definitions.
 */
@FunctionalInterface
public interface PropertyFactory {
    /**
     * Constructs a specialized property instance matching structural coordinates.
     *
     * @param resourceName The exact registry identifier of the property.
     * @param group        The parent group structural binding.
     * @param defaultValue The baseline value applied prior to network sync or disk loading.
     * @param min          The lower validation boundary. Nullable depending on implementation constraints.
     * @param max          The upper validation boundary. Nullable depending on implementation constraints.
     * @return A dynamically allocated configuration property instance.
     */
    Property<?> create(String resourceName, Group group, Object defaultValue, Object min, Object max);
}