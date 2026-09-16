package johnsmith.configoverhauled.api;

import com.mojang.serialization.Codec;

import java.util.List;

import johnsmith.configoverhauled.api.data.ConfigScope;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Defines a structural subdivision within a category.
 * Serves as the primary registration container for configuration properties via a fluent builder interface.
 */
public interface Group {
    /**
     * Retrieves the parent category bounding this group.
     *
     * @return The Category instance.
     */
    Category parent();

    /**
     * Retrieves the structural identifier of the group.
     *
     * @return The unique string identifier.
     */
    String id();

    /**
     * Retrieves the configuration manager authoritative over this group.
     *
     * @return The ConfigManager instance.
     */
    ConfigManager manager();

    /**
     * Retrieves the base localization key utilized for client-side GUI translation.
     *
     * @return The formatted translation key string.
     */
    String translationKey();

    /**
     * Severs the structural bond between this group and the specified property.
     *
     * @param property The target property to remove.
     */
    void remove(Property<?> property);

    /**
     * Evaluates the active state of the group's registry.
     *
     * @return True if no properties are currently bound to this group.
     */
    boolean isEmpty();

    /**
     * Initiates the fluent builder sequence to construct and bind a new property to this group.
     *
     * @param resourceName The precise registry identifier for the intended property.
     * @return The initial scope resolution step of the builder.
     */
    IScopeStep define(String resourceName);

    /**
     * Phase 1 of the fluent builder: Determines the network and lifecycle scope of the property.
     */
    interface IScopeStep {
        /**
         * Assigns a generalized scope.
         *
         * @param scope The target ConfigScope.
         * @return The subsequent type resolution step.
         */
        ITypeStep withScope(ConfigScope scope);

        /**
         * Assigns the CLIENT scope.
         *
         * @return The subsequent type resolution step.
         */
        ITypeStep clientSide();

        /**
         * Assigns the LEVEL scope.
         *
         * @return The subsequent type resolution step.
         */
        ITypeStep levelSide();

        /**
         * Assigns the GLOBAL scope.
         *
         * @return The subsequent type resolution step.
         */
        ITypeStep globalSide();
    }

    /**
     * Phase 2 of the fluent builder: Determines the fundamental data type, baseline state, and validation bounds.
     */
    interface ITypeStep {
        /**
         * Binds the property to a boolean state.
         *
         * @param defaultValue The baseline boolean state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Boolean> asBoolean(boolean defaultValue);

        /**
         * Binds the property to an unbounded integer value.
         *
         * @param defaultValue The baseline integer state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Integer> asInteger(int defaultValue);

        /**
         * Binds the property to a bounded integer value.
         *
         * @param defaultValue The baseline integer state.
         * @param min          The lower validation boundary.
         * @param max          The upper validation boundary.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Integer> asInteger(int defaultValue, int min, int max);

        /**
         * Binds the property to an unbounded double value.
         *
         * @param defaultValue The baseline double state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Double> asDouble(double defaultValue);

        /**
         * Binds the property to a bounded double value.
         *
         * @param defaultValue The baseline double state.
         * @param min          The lower validation boundary.
         * @param max          The upper validation boundary.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Double> asDouble(double defaultValue, double min, double max);

        /**
         * Binds the property to an unbounded float value.
         *
         * @param defaultValue The baseline float state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Float> asFloat(float defaultValue);

        /**
         * Binds the property to a bounded float value.
         *
         * @param defaultValue The baseline float state.
         * @param min          The lower validation boundary.
         * @param max          The upper validation boundary.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Float> asFloat(float defaultValue, float min, float max);

        /**
         * Binds the property to an unbounded long value.
         *
         * @param defaultValue The baseline long state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Long> asLong(long defaultValue);

        /**
         * Binds the property to a bounded long value.
         *
         * @param defaultValue The baseline long state.
         * @param min          The lower validation boundary.
         * @param max          The upper validation boundary.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Long> asLong(long defaultValue, long min, long max);

        /**
         * Binds the property to an RGB color integer.
         *
         * @param defaultValue The baseline color state as a primitive integer.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Integer> asRGBColor(int defaultValue);

        /**
         * Binds the property to an ARGB color integer.
         *
         * @param defaultValue The baseline color state as a primitive integer.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Integer> asARGBColor(int defaultValue);

        /**
         * Binds the property to a string literal.
         *
         * @param defaultValue The baseline string state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<String> asString(String defaultValue);

        /**
         * Binds the property to an enumerated type.
         *
         * @param defaultValue The baseline enumeration state.
         * @param codec        The serialization protocol for the enumeration.
         * @return The subsequent optional configuration step.
         */
        <E extends Enum<E>> IPropertyBuilder<E> asEnum(E defaultValue, Codec<E> codec);

        /**
         * Binds the property to a uniform list of elements.
         *
         * @param defaultValue The baseline list state.
         * @param elementCodec The serialization protocol for individual elements.
         * @return The subsequent optional configuration step.
         */
        <E> IPropertyBuilder<List<E>> asList(List<E> defaultValue, Codec<E> elementCodec);

        /**
         * Binds the property to a structural Block registry reference.
         *
         * @param defaultValue The baseline Block state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Block> asBlock(Block defaultValue);

        /**
         * Binds the property to an Item registry reference.
         *
         * @param defaultValue The baseline Item state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<Item> asItem(Item defaultValue);

        /**
         * Binds the property to a list of Block registry references.
         *
         * @param defaultValue The baseline Block list state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<List<Block>> asBlocks(List<Block> defaultValue);

        /**
         * Binds the property to a list of Item registry references.
         *
         * @param defaultValue The baseline Item list state.
         * @return The subsequent optional configuration step.
         */
        IPropertyBuilder<List<Item>> asItems(List<Item> defaultValue);
    }

    /**
     * Phase 3 of the fluent builder: Permits assignment of non-critical metadata and executes the terminal registration.
     *
     * @param <T> The data type managed by the pending property.
     * @param <B> The self-referential builder implementation.
     */
    interface IOptionalStep<T, B extends IOptionalStep<T, B>> {
        /**
         * Injects a descriptive text sequence for disk serialization.
         *
         * @param comment The documentation string.
         * @return The current builder sequence.
         */
        B withComment(String comment);

        /**
         * Overrides the default localization key trajectory.
         *
         * @param translationKey The specialized translation key.
         * @return The current builder sequence.
         */
        B withTranslationKey(String translationKey);

        /**
         * Terminates the builder sequence, allocates the property instance, and binds it to the active ConfigManager.
         *
         * @return The constructed and synchronized Property instance.
         */
        Property<T> register();
    }

    /**
     * Resolves the generic self-referential boundary of IOptionalStep.
     * Serves as the concrete return type for the terminal phase of the standard property builder sequence.
     *
     * @param <T> The data type managed by the pending property.
     */
    interface IPropertyBuilder<T> extends IOptionalStep<T, IPropertyBuilder<T>> {}
}