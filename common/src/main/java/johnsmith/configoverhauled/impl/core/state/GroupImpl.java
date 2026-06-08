package johnsmith.configoverhauled.impl.core.state;

import com.mojang.serialization.Codec;
import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.data.ConfigScope;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;

public record GroupImpl(Category parent, String id, ConfigManager manager) implements Group {
    @Override
    public String translationKey() {
        return parent.translationKey() + "." + this.id;
    }

    @Override
    public void remove(Property<?> property) {
        this.manager().remove(property);
        if (this.isEmpty()) this.parent().remove(this);
    }

    @Override
    public boolean isEmpty() {
        return this.manager().getPropertiesIn(this).isEmpty();
    }

    @Override
    public IScopeStep define(String resourceName) {
        return new BuilderPipeline(this, resourceName);
    }

    private static class BuilderPipeline implements IScopeStep, ITypeStep {
        private final GroupImpl group;
        private final String resourceName;
        private ConfigScope scope;

        private BuilderPipeline(GroupImpl group, String resourceName) {
            this.group = group;
            this.resourceName = resourceName;
        }

        @Override
        public ITypeStep withScope(ConfigScope scope) {
            this.scope = scope;
            return this;
        }

        @Override public ITypeStep clientSide() { return withScope(ConfigScope.CLIENT); }
        @Override public ITypeStep levelSide() { return withScope(ConfigScope.LEVEL); }
        @Override public ITypeStep globalSide() { return withScope(ConfigScope.GLOBAL); }

        @Override
        public IPropertyBuilder<Boolean> asBoolean(boolean defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Boolean(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<Integer> asInteger(int defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Integer(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<Integer> asInteger(int defaultValue, int min, int max) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Integer(resourceName, group, scope, comment, defaultValue, min, max, false));
        }

        @Override
        public IPropertyBuilder<Double> asDouble(double defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Double(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<Double> asDouble(double defaultValue, double min, double max) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Double(resourceName, group, scope, comment, defaultValue, min, max, false));
        }

        @Override
        public IPropertyBuilder<Float> asFloat(float defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Float(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<Float> asFloat(float defaultValue, float min, float max) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Float(resourceName, group, scope, comment, defaultValue, min, max, false));
        }

        @Override
        public IPropertyBuilder<Long> asLong(long defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Long(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<Long> asLong(long defaultValue, long min, long max) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Long(resourceName, group, scope, comment, defaultValue, min, max, false));
        }

        @Override
        public IPropertyBuilder<Integer> asColor(int defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Color(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<String> asString(String defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.String(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public <E extends Enum<E>> IPropertyBuilder<E> asEnum(E defaultValue, Codec<E> codec) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Enum<>(resourceName, group, scope, comment, defaultValue, codec, false));
        }

        @Override
        public <E> IPropertyBuilder<List<E>> asList(List<E> defaultValue, Codec<E> elementCodec) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.List<>(resourceName, group, scope, comment, defaultValue, elementCodec, false));
        }

        @Override
        public IPropertyBuilder<Block> asBlock(Block defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Block(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<Item> asItem(Item defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Item(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<List<Block>> asBlocks(List<Block> defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Blocks(resourceName, group, scope, comment, defaultValue, false));
        }

        @Override
        public IPropertyBuilder<List<Item>> asItems(List<Item> defaultValue) {
            return new PropertyBuilderImpl<>(group, comment -> new PropertyImpl.Items(resourceName, group, scope, comment, defaultValue, false));
        }
    }

    private static class PropertyBuilderImpl<T> implements IPropertyBuilder<T> {
        private final GroupImpl group;
        private final Function<String, PropertyImpl<T>> factory;
        private String comment = "";

        private PropertyBuilderImpl(GroupImpl group, Function<String, PropertyImpl<T>> factory) {
            this.group = group;
            this.factory = factory;
        }

        @Override
        public IPropertyBuilder<T> withComment(String comment) {
            this.comment = comment;
            return this;
        }

        @Override
        public IPropertyBuilder<T> withTranslationKey(String key) {
            return this;
        }

        @Override
        public Property<T> register() {
            PropertyImpl<T> property = factory.apply(this.comment);
            this.group.manager().registerProperty(property);
            return property;
        }
    }
}