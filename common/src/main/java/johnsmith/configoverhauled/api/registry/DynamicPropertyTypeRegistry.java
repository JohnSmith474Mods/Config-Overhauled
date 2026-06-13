package johnsmith.configoverhauled.api.registry;

import com.mojang.serialization.Codec;
import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import johnsmith.configoverhauled.api.factory.PropertyFactory;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for dynamic configuration property types.
 * Maps namespaced string identifiers to their respective {@link TypeDefinition} instances,
 * enabling the dynamic parsing and instantiation of properties at runtime.
 */
public final class DynamicPropertyTypeRegistry {
    private static final Map<String, TypeDefinition<?>> REGISTRY = new ConcurrentHashMap<>();

    /** Represents a dynamically instantiable floating-point property. */
    public static final TypeDefinition<Float> FLOAT = register(Constants.MOD_ID, "float", Codec.FLOAT, (name, group, def, min, max, codec) -> new DefaultProperty.Float(name, group, ConfigScope.LEVEL, null, (Float) def, (Float) min, (Float) max, true));
    /** Represents a dynamically instantiable integer property. */
    public static final TypeDefinition<Integer> INTEGER = register(Constants.MOD_ID, "integer", Codec.INT, (name, group, def, min, max, codec) -> new DefaultProperty.Integer(name, group, ConfigScope.LEVEL, null, (Integer) def, (Integer) min, (Integer) max, true));
    /** Represents a dynamically instantiable double-precision floating-point property. */
    public static final TypeDefinition<Double> DOUBLE = register(Constants.MOD_ID, "double", Codec.DOUBLE, (name, group, def, min, max, codec) -> new DefaultProperty.Double(name, group, ConfigScope.LEVEL, null, (Double) def, (Double) min, (Double) max, true));
    /** Represents a dynamically instantiable long integer property. */
    public static final TypeDefinition<Long> LONG = register(Constants.MOD_ID, "long", Codec.LONG, (name, group, def, min, max, codec) -> new DefaultProperty.Long(name, group, ConfigScope.LEVEL, null, (Long) def, (Long) min, (Long) max, true));
    /** Represents a dynamically instantiable 24-bit RGB color property. */
    public static final TypeDefinition<Integer> RGB_COLOR = register(Constants.MOD_ID, "rgb_color", Codec.INT, (name, group, def, min, max, codec) -> new DefaultProperty.RGBColor(name, group, ConfigScope.LEVEL, null, (Integer) def, true));
    /** Represents a dynamically instantiable 32-bit ARGB color property. */
    public static final TypeDefinition<Integer> ARGB_COLOR = register(Constants.MOD_ID, "argb_color", Codec.INT, (name, group, def, min, max, codec) -> new DefaultProperty.ARGBColor(name, group, ConfigScope.LEVEL, null, (Integer) def, true));
    /** Represents a dynamically instantiable boolean property. */
    public static final TypeDefinition<Boolean> BOOLEAN = register(Constants.MOD_ID, "boolean", Codec.BOOL, (name, group, def, min, max, codec) -> new DefaultProperty.Boolean(name, group, ConfigScope.LEVEL, null, (Boolean) def, true));
    /** Represents a dynamically instantiable string property. */
    public static final TypeDefinition<String> STRING = register(Constants.MOD_ID, "string", Codec.STRING, (name, group, def, min, max, codec) -> new DefaultProperty.String(name, group, ConfigScope.LEVEL, null, (String) def, true));
    /** Represents a dynamically instantiable Minecraft Block property. */
    public static final TypeDefinition<Block> BLOCK = register(Constants.MOD_ID, "block", BuiltInRegistries.BLOCK.byNameCodec(), (name, group, def, min, max, codec) -> new DefaultProperty.Block(name, group, ConfigScope.LEVEL, null, (Block) def, true));
    /** Represents a dynamically instantiable Minecraft Item property. */
    public static final TypeDefinition<Item> ITEM = register(Constants.MOD_ID, "item", BuiltInRegistries.ITEM.byNameCodec(), (name, group, def, min, max, codec) -> new DefaultProperty.Item(name, group, ConfigScope.LEVEL, null, (Item) def, true));
    /** Represents a dynamically instantiable list of Minecraft Blocks property. */
    public static final TypeDefinition<List<Block>> BLOCKS = register(Constants.MOD_ID, "blocks", Codec.list(BuiltInRegistries.BLOCK.byNameCodec()), (name, group, def, min, max, codec) -> new DefaultProperty.Blocks(name, group, ConfigScope.LEVEL, null, (List<Block>) def, true));
    /** Represents a dynamically instantiable list of Minecraft Items property. */
    public static final TypeDefinition<List<Item>> ITEMS = register(Constants.MOD_ID, "items", Codec.list(BuiltInRegistries.ITEM.byNameCodec()), (name, group, def, min, max, codec) -> new DefaultProperty.Items(name, group, ConfigScope.LEVEL, null, (List<Item>) def, true));

    /**
     * Encapsulates the configuration requirements for a specific property type.
     *
     * @param id      The absolute namespaced identifier (e.g., "modid:type").
     * @param codec   The codec used for serialization and deserialization of the property value.
     * @param factory The factory responsible for instantiating the property state.
     * @param <T>     The underlying data type managed by this property definition.
     */
    public record TypeDefinition<T>(String id, Codec<T> codec, PropertyFactory<T> factory) {
        /**
         * Instantiates a new dynamic property based on this type definition.
         *
         * @param name  The internal name of the property.
         * @param group The parent group containing this property.
         * @param def   The default value. Expected to cast safely to {@code <T>}.
         * @param min   The lower bound limit, if applicable.
         * @param max   The upper bound limit, if applicable.
         * @return A constructed {@link Property} instance.
         */
        public Property<T> create(String name, Group group, Object def, Object min, Object max) {
            return factory.create(name, group, def, min, max, codec);
        }
    }

    /**
     * Registers a new dynamic property type definition.
     *
     * @param modId   The namespace ID of the mod registering the type.
     * @param typeId  The unique identifier for the type within the namespace.
     * @param codec   The codec responsible for processing the underlying data type.
     * @param factory The factory sequence executing the property construction.
     * @param <T>     The underlying data type.
     * @return The registered {@link TypeDefinition}.
     * @throws IllegalArgumentException If either the modId or typeId is null.
     */
    public static <T> TypeDefinition<T> register(String modId, String typeId, Codec<T> codec, PropertyFactory<T> factory) {
        if (modId == null || typeId == null) throw new IllegalArgumentException("Namespace components cannot be null.");
        String absoluteId = modId + ":" + typeId;
        TypeDefinition<T> typeDefinition = new TypeDefinition<>(absoluteId, codec, factory);
        REGISTRY.putIfAbsent(absoluteId, typeDefinition);
        return typeDefinition;
    }

    /**
     * Retrieves a registered property type definition by its absolute identifier.
     *
     * @param id The absolute namespaced identifier (e.g., "modid:type").
     * @return The matching {@link TypeDefinition}.
     * @throws IllegalStateException If the requested identifier is not registered.
     */
    public static TypeDefinition<?> get(String id) {
        TypeDefinition<?> definition = REGISTRY.get(id);
        if (definition == null) {
            throw new IllegalStateException("Unregistered dynamic property identifier: " + id);
        }
        return definition;
    }
}