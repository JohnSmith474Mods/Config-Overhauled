package johnsmith.configoverhauled.impl.client.gui.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.entry.ConfigEntry;
import johnsmith.configoverhauled.api.client.gui.factory.WidgetFactory;
import johnsmith.configoverhauled.api.client.gui.registry.WidgetRegistry;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import johnsmith.configoverhauled.impl.client.gui.entry.*;
import johnsmith.configoverhauled.impl.client.gui.entry.bounded.*;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.BlockEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.BlockListEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.ItemEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.ItemListEntry;
import johnsmith.configoverhauled.impl.core.state.DefaultProperty;

import net.minecraft.client.Minecraft;

public class DefaultWidgetRegistry implements WidgetRegistry {
    private final Map<Class<?>, WidgetFactory<?>> registry = new ConcurrentHashMap<>();

    public DefaultWidgetRegistry() {
        this.registerDefaults();
    }

    public <P extends Property<?>> void register(Class<P> propertyClass, WidgetFactory<? super P> factory) {
        this.registry.putIfAbsent(propertyClass, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ConfigEntry createEntry(Property<T> property, ConfigScreen screen, Minecraft mc, Runnable callback) {
        WidgetFactory<Property<T>> factory = (WidgetFactory<Property<T>>) this.registry.get(property.getClass());
        if (factory == null) {
            throw new IllegalStateException("No widget factory registered for property type: " + property.getClass().getName());
        }
        return factory.create(property, screen, mc, callback);
    }

    private void registerDefaults() {
        this.register(DefaultProperty.Boolean.class, BooleanEntry::new);
        this.register(DefaultProperty.RGBColor.class, RGBColorEntry::new);
        this.register(DefaultProperty.ARGBColor.class, ARGBColorEntry::new);
        this.register(DefaultProperty.Double.class, DoubleEntry::new);
        this.register(DefaultProperty.Float.class, FloatEntry::new);
        this.register(DefaultProperty.Integer.class, IntegerEntry::new);
        this.register(DefaultProperty.Long.class, LongEntry::new);
        this.register(DefaultProperty.String.class, StringEntry::new);
        this.register(DefaultProperty.Enum.class, (WidgetFactory) EnumEntry::new);
        this.register(DefaultProperty.List.class, (WidgetFactory) ListEntry::new);
        this.register(DefaultProperty.Block.class, (WidgetFactory) BlockEntry::new);
        this.register(DefaultProperty.Item.class, (WidgetFactory) ItemEntry::new);
        this.register(DefaultProperty.Blocks.class, (WidgetFactory) BlockListEntry::new);
        this.register(DefaultProperty.Items.class, (WidgetFactory) ItemListEntry::new);
    }
}