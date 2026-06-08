package johnsmith.configoverhauled.impl.core.state;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import johnsmith.configoverhauled.Constants;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public abstract sealed class PropertyImpl<T> implements Property<T> permits PropertyImpl.Boolean, PropertyImpl.Bounded, PropertyImpl.Enum, PropertyImpl.List, PropertyImpl.String, PropertyImpl.Block, PropertyImpl.Item {
    private final java.lang.String resourceName;
    private final Group parentGroup;
    private final T defaultValue;
    private final Codec<T> codec;
    private final ConfigScope scope;
    private final java.lang.String comment;
    private final boolean isDynamic;

    private T diskValue;
    private volatile T runtimeValue;
    private boolean isSynced = false;
    private final Set<Listener> listeners = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    protected PropertyImpl(java.lang.String resourceName, Group category, ConfigScope scope, java.lang.String comment, T defaultValue, Codec<T> codec, boolean isDynamic) {
        this.resourceName = resourceName;
        this.parentGroup = category;
        this.scope = scope;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.diskValue = defaultValue;
        this.runtimeValue = defaultValue;
        this.codec = codec;
        this.isDynamic = isDynamic;
    }

    @Override public java.lang.String resourceName() { return this.resourceName; }
    @Override public Group parentGroup() { return this.parentGroup; }
    @Override public T defaultValue() { return this.defaultValue; }
    @Override public Codec<T> codec() { return this.codec; }
    @Override public ConfigScope scope() { return this.scope; }
    @Override public java.lang.String comment() { return this.comment; }
    @Override public boolean isDynamic() { return this.isDynamic; }

    @Override
    public java.lang.String translationKey() {
        return this.parentGroup.translationKey() + "." + resourceName;
    }

    @Override
    public java.lang.String descriptionTranslationKey() {
        return this.translationKey() + ".tooltip";
    }

    @Override
    public java.lang.String getUniqueId() {
        return this.parentGroup.parent().id() + "." + this.parentGroup.id() + "." + this.resourceName;
    }

    @Override
    public T get() {
        return runtimeValue;
    }

    @Override
    public void set(T newValue) {
        T validated = validate(newValue);
        this.diskValue = validated;
        if (!isSynced) this.runtimeValue = validated;
        this.notifyChange();
    }

    @Override
    public void acceptAuthoritativeUpdate(Tag tag) {
        this.codec.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> Constants.LOG.error("Failed to apply OP edit to '{}': {}", this.getUniqueId(), error))
                .ifPresent(val -> {
                    T validated = validate(val);
                    this.diskValue = validated;
                    this.runtimeValue = validated;
                    this.notifyChange();
                });
    }

    @Override
    public void onSync(T serverValue) {
        if (this.scope == ConfigScope.CLIENT) {
            Constants.LOG.warn("Ignored server attempt to sync CLIENT-scoped property '{}'.", this.getUniqueId());
            return;
        }
        this.runtimeValue = validate(serverValue);
        this.isSynced = true;
        this.notifyChange();
    }

    @Override
    public void onDisconnect() {
        this.runtimeValue = this.diskValue;
        this.isSynced = false;
        this.invalidate();
        this.listeners.clear();
        if (this.scope == ConfigScope.LEVEL && this.isDynamic) {
            Constants.LOG.info("Removing dynamic LEVEL scoped property: {}", this.getUniqueId());
            this.parentGroup.remove(this);
        }
    }

    @Override
    public Tag encodeValueToNbt() {
        return this.codec.encodeStart(NbtOps.INSTANCE, this.get())
                .getOrThrow(error -> new IllegalStateException("Failed to encode property " + this.getUniqueId() + ": " + error));
    }

    @Override
    public void decodeValueFromNbt(Tag tag) {
        this.codec.parse(NbtOps.INSTANCE, tag)
                .resultOrPartial(error -> Constants.LOG.error("Failed to decode property '{}': {}", this.getUniqueId(), error))
                .ifPresent(this::onSync);
    }

    protected T validate(T value) { return value; }

    @Override
    public void load(UnmodifiableCommentedConfig config) {
        java.lang.String path = this.getUniqueId();
        if (!config.contains(path)) return;
        var rawValue = config.get(path);
        DataResult<T> result = this.codec.parse(JavaOps.INSTANCE, rawValue);

        result.resultOrPartial(error -> Constants.LOG.error("Failed to load config '{}': {}", path, error))
                .ifPresent(val -> {
                    this.diskValue = validate(val);
                    if (!isSynced) this.runtimeValue = this.diskValue;
                });
    }

    @Override
    public void save(CommentedConfig config) {
        if (config == null) return;
        java.lang.String path = this.getUniqueId();
        DataResult<Object> result = this.codec.encodeStart(JavaOps.INSTANCE, this.diskValue);

        result.resultOrPartial(error -> Constants.LOG.error("Failed to save config '{}': {}", path, error))
                .ifPresent(serialized -> {
                    config.set(path, serialized);
                    config.setComment(path,
                            buildFormattedComment());
                });
    }

    protected java.lang.String buildFormattedComment() {
        StringBuilder builder = new StringBuilder();
        if (this.comment != null && !this.comment.isEmpty()) {
            builder.append(" ").append(this.comment).append("\n");
        }
        appendDefaultValueComment(builder);
        return builder.toString();
    }

    protected void appendDefaultValueComment(StringBuilder builder) {
        builder.append(" Default: ").append(this.defaultValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Property<?> other)) return false;
        return this.getUniqueId().equals(other.getUniqueId());
    }

    @Override
    public int hashCode() { return this.getUniqueId().hashCode(); }

    @Override
    public void addListener(Listener listener) { this.listeners.add(listener); }

    @Override
    public void removeListener(Listener listener) { this.listeners.remove(listener); }

    protected void invalidate() {
        synchronized (this.listeners) {
            for (Listener listener : listeners) listener.onPropertyInvalidated();
        }
    }

    protected void notifyChange() {
        synchronized (this.listeners) {
            for (Listener listener : listeners) listener.onPropertyChanged();
        }
    }

    public static non-sealed abstract class Bounded<T extends Number & Comparable<T>> extends PropertyImpl<T> {
        public final T lowerBound;
        public final T upperBound;

        public Bounded(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, T defaultValue, T lowerBound, T upperBound, Codec<T> codec, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, codec, isDynamic);
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        protected T validate(T value) {
            if (value.compareTo(lowerBound) < 0) return lowerBound;
            if (value.compareTo(upperBound) > 0) return upperBound;
            return value;
        }

        protected java.lang.String formatBound(T value) {
            return value + "";
        }

        @Override
        protected java.lang.String buildFormattedComment() {
            return super.buildFormattedComment() + "\n Range: " + this.formatBound(this.lowerBound) + " to " + this.formatBound(this.upperBound);
        }
    }

    public static final class Boolean extends PropertyImpl<java.lang.Boolean> {
        public Boolean(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Boolean defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, Codec.BOOL, isDynamic);
        }
    }

    public static final class Color extends Bounded<java.lang.Integer> {
        public Color(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Integer defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, johnsmith.configoverhauled.api.data.Color.LOWER_BOUND, johnsmith.configoverhauled.api.data.Color.UPPER_BOUND, johnsmith.configoverhauled.api.data.Color.CODEC, isDynamic);
        }

        @Override
        protected java.lang.String formatBound(java.lang.Integer value) {
            return java.lang.String.format("#%06X", value);
        }
    }

    public static final class Long extends Bounded<java.lang.Long> {
        public Long(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Long defaultValue, java.lang.Long lowerBound, java.lang.Long upperBound, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, lowerBound, upperBound, Codec.LONG, isDynamic);
        }
        public Long(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Long defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, java.lang.Long.MIN_VALUE, java.lang.Long.MAX_VALUE, Codec.LONG, isDynamic);
        }
    }

    public static final class Integer extends Bounded<java.lang.Integer> {
        public Integer(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Integer defaultValue, java.lang.Integer lowerBound, java.lang.Integer upperBound, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, lowerBound, upperBound, Codec.INT, isDynamic);
        }
        public Integer(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Integer defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, java.lang.Integer.MIN_VALUE, java.lang.Integer.MAX_VALUE, Codec.INT, isDynamic);
        }
    }

    public static final class Double extends Bounded<java.lang.Double> {
        public Double(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Double defaultValue, java.lang.Double lowerBound, java.lang.Double upperBound, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, lowerBound, upperBound, Codec.DOUBLE, isDynamic);
        }
        public Double(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Double defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, -java.lang.Double.MAX_VALUE, java.lang.Double.MAX_VALUE, Codec.DOUBLE, isDynamic);
        }
    }

    public static final class Float extends Bounded<java.lang.Float> {
        public Float(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Float defaultValue, java.lang.Float lowerBound, java.lang.Float upperBound, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, lowerBound, upperBound, Codec.FLOAT, isDynamic);
        }
        public Float(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.Float defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, -java.lang.Float.MAX_VALUE, java.lang.Float.MAX_VALUE, Codec.FLOAT, isDynamic);
        }
    }

    public static final class String extends PropertyImpl<java.lang.String> {
        public String(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.lang.String defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, Codec.STRING, isDynamic);
        }
    }

    public static final class Enum<E extends java.lang.Enum<E>> extends PropertyImpl<E> {
        public Enum(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, E defaultValue, Codec<E> codec, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, codec, isDynamic);
        }
    }

    public static non-sealed class List<E> extends PropertyImpl<java.util.List<E>> {
        private final Codec<E> elementCodec;

        public List(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.util.List<E> defaultValue, Codec<E> elementCodec, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, elementCodec.listOf(), isDynamic);
            this.elementCodec = elementCodec;
        }

        public Codec<E> elementCodec() {
            return this.elementCodec;
        }

        @Override
        protected void appendDefaultValueComment(StringBuilder builder) {
            this.codec().encodeStart(JsonOps.INSTANCE, this.defaultValue())
                    .result()
                    .ifPresent(jsonElement -> {
                        builder.append(" Default: ").append(jsonElement.toString());
                    });
        }
    }

    public static final class Block extends PropertyImpl<net.minecraft.world.level.block.Block> {
        public Block(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, net.minecraft.world.level.block.Block defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, net.minecraft.core.registries.BuiltInRegistries.BLOCK.byNameCodec(), isDynamic);
        }
    }

    public static final class Item extends PropertyImpl<net.minecraft.world.item.Item> {
        public Item(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, net.minecraft.world.item.Item defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec(), isDynamic);
        }
    }

    public static final class Blocks extends List<net.minecraft.world.level.block.Block> {
        public Blocks(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.util.List<net.minecraft.world.level.block.Block> defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, net.minecraft.core.registries.BuiltInRegistries.BLOCK.byNameCodec(), isDynamic);
        }
    }

    public static final class Items extends List<net.minecraft.world.item.Item> {
        public Items(java.lang.String resourceName, Group group, ConfigScope scope, java.lang.String comment, java.util.List<net.minecraft.world.item.Item> defaultValue, boolean isDynamic) {
            super(resourceName, group, scope, comment, defaultValue, net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec(), isDynamic);
        }
    }
}