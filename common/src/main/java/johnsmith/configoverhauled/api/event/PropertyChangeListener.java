package johnsmith.configoverhauled.api.event;

/**
 * Defines callback hooks for configuration property state transitions.
 * Implementations must register via the target property's listener collection.
 */
public interface PropertyChangeListener {
    /**
     * Invoked when the property instance becomes orphaned or structurally detached.
     * Indicates the underlying state is no longer authoritative or synchronized.
     */
    void onPropertyInvalidated();

    /**
     * Invoked immediately after the property's runtime value is modified.
     * Triggered by local disk updates, authoritative server synchronization, or programmatic mutation.
     */
    void onPropertyChanged();
}