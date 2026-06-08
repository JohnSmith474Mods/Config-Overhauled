package johnsmith.configoverhauled.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.mojang.serialization.Codec;

import johnsmith.configoverhauled.api.data.ConfigScope;

import net.minecraft.nbt.Tag;

/**
 * Represents a discrete, manageable configuration value within the structural hierarchy.
 * Handles state encapsulation, disk serialization, network synchronization, and lifecycle broadcasting.
 *
 * @param <T> The foundational data type managed by this property.
 */
public interface Property<T> {
    /**
     * Retrieves the exact registry identifier of the property.
     *
     * @return The resource string.
     */
    String resourceName();

    /**
     * Retrieves the structural group encapsulating this property.
     *
     * @return The bound Group instance.
     */
    Group parentGroup();

    /**
     * Retrieves the baseline state assigned during instantiation prior to modifications or disk loads.
     *
     * @return The initial default value.
     */
    T defaultValue();

    /**
     * Retrieves the serialization protocol required to translate the property state to and from external formats.
     *
     * @return The bound Codec instance.
     */
    Codec<T> codec();

    /**
     * Retrieves the operational boundary and synchronization policy governing the property.
     *
     * @return The designated ConfigScope.
     */
    ConfigScope scope();

    /**
     * Retrieves the supplementary text sequence injected during disk serialization.
     *
     * @return The documentation string, or null if unassigned.
     */
    String comment();

    /**
     * Determines if the property was constructed dynamically at runtime rather than via static allocation.
     *
     * @return True if the property was dynamically injected.
     */
    boolean isDynamic();

    /**
     * Retrieves the base localization key utilized for client-side GUI title translation.
     *
     * @return The formatted translation key.
     */
    String translationKey();

    /**
     * Retrieves the extended localization key utilized for client-side GUI tooltip translation.
     *
     * @return The formatted tooltip key.
     */
    String descriptionTranslationKey();

    /**
     * Constructs the absolute coordinate string combining the parent category, group, and resource name.
     *
     * @return The hierarchical ID string.
     */
    String getUniqueId();

    /**
     * Retrieves the active runtime state.
     *
     * @return The current memory value.
     */
    T get();

    /**
     * Mutates the active runtime and disk states, automatically broadcasting the transition to registered listeners.
     *
     * @param newValue The requested state mutation.
     */
    void set(T newValue);

    /**
     * Forces a localized state override originating from an authoritative server command or operator edit.
     *
     * @param tag The serialized NBT payload representing the authoritative state.
     */
    void acceptAuthoritativeUpdate(Tag tag);

    /**
     * Applies an incoming network state sequence to the runtime value and sets the synchronization flag.
     *
     * @param serverValue The deserialized value transmitted by the authoritative server.
     */
    void onSync(T serverValue);

    /**
     * Executes local state teardown. Purges network-assigned runtime values, drops listeners, and detaches dynamic instances.
     */
    void onDisconnect();

    /**
     * Translates the active runtime value into a network-transmittable NBT payload.
     *
     * @return The serialized Tag sequence.
     */
    Tag encodeValueToNbt();

    /**
     * Mutates the runtime state by parsing an incoming network transmission payload.
     *
     * @param tag The serialized NBT state data.
     */
    void decodeValueFromNbt(Tag tag);

    /**
     * Extracts and applies the corresponding disk definition to the internal state variables.
     *
     * @param config The structural disk model holding the property value.
     */
    void load(UnmodifiableCommentedConfig config);

    /**
     * Injects the localized disk state and documentation string into the designated file format sequence.
     *
     * @param config The target structural disk model.
     */
    void save(CommentedConfig config);

    /**
     * Binds an external listener sequence to the property's lifecycle broadcast collection.
     *
     * @param listener The callback implementation.
     */
    void addListener(Listener listener);

    /**
     * Detaches an active listener sequence from the property's broadcast collection.
     *
     * @param listener The callback implementation to purge.
     */
    void removeListener(Listener listener);

    /**
     * Defines callback hooks for internal state transitions.
     */
    interface Listener {
        /**
         * Triggered when the parent property detaches from the structural hierarchy or severs an authoritative connection.
         */
        void onPropertyInvalidated();

        /**
         * Triggered upon localized disk state modifications or successful authoritative network synchronization.
         */
        void onPropertyChanged();
    }
}