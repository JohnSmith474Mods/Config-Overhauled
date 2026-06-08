package johnsmith.configoverhauled.api;

/**
 * Represents a top-level structural classification within the configuration hierarchy.
 * Organizes subsidiary property groups.
 */
public interface Category {
    /**
     * Retrieves the structural identifier of the category.
     *
     * @return The unique string identifier.
     */
    String id();

    /**
     * Retrieves the configuration manager authoritative over this category.
     *
     * @return The parent ConfigManager instance.
     */
    ConfigManager manager();

    /**
     * Retrieves the base localization key utilized for client-side GUI translation.
     *
     * @return The formatted translation key string.
     */
    String translationKey();

    /**
     * Allocates or retrieves a subsidiary structural group within this category.
     *
     * @param id The target group identifier.
     * @return The bound Group instance.
     */
    Group define(String id);

    /**
     * Severs the structural bond between this category and the specified group.
     *
     * @param group The target group to remove.
     */
    void remove(Group group);

    /**
     * Evaluates the active state of the category's structural hierarchy.
     *
     * @return True if no subsidiary groups are registered.
     */
    boolean isEmpty();
}