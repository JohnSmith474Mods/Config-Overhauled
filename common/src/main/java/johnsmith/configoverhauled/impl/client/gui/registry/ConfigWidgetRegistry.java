package johnsmith.configoverhauled.impl.client.gui.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.entry.*;
import johnsmith.configoverhauled.impl.client.gui.entry.bounded.*;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.BlockEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.BlockListEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.ItemEntry;
import johnsmith.configoverhauled.impl.client.gui.entry.registry.ItemListEntry;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import johnsmith.configoverhauled.impl.core.state.PropertyImpl;

import net.minecraft.client.Minecraft;

public class ConfigWidgetRegistry {
    @FunctionalInterface
    public interface WidgetFactory<P extends Property<?>> {
        OptionEntry<?, ?> create(P property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged);
    }

    private static final Map<Class<?>, WidgetFactory<?>> REGISTRY = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static <P extends Property<?>> void register(Class<P> propertyClass, WidgetFactory<P> factory) {
        REGISTRY.putIfAbsent(propertyClass, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T> OptionEntry<T, ?> createEntry(Property<T> property, AbstractConfigScreen screen, Minecraft mc, Runnable callback) {
        WidgetFactory<Property<T>> factory = (WidgetFactory<Property<T>>) REGISTRY.get(property.getClass());
        if (factory == null) {
            throw new IllegalStateException("No widget factory registered for property type: " + property.getClass().getName());
        }
        return (OptionEntry<T, ?>) factory.create(property, screen, mc, callback);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerWidgets() {
        if (initialized) return;

        register(PropertyImpl.Boolean.class, BooleanEntry::new);
        register(PropertyImpl.Color.class, ColorEntry::new);
        register(PropertyImpl.Double.class, DoubleEntry::new);
        register(PropertyImpl.Float.class, FloatEntry::new);
        register(PropertyImpl.Integer.class, IntegerEntry::new);
        register(PropertyImpl.Long.class, LongEntry::new);
        register(PropertyImpl.String.class, StringEntry::new);
        register(PropertyImpl.Enum.class, (WidgetFactory) EnumEntry::new);
        register(PropertyImpl.List.class, (WidgetFactory) ListEntry::new);
        register(PropertyImpl.Block.class, (WidgetFactory) BlockEntry::new);
        register(PropertyImpl.Item.class, (WidgetFactory) ItemEntry::new);
        register(PropertyImpl.Blocks.class, (WidgetFactory) BlockListEntry::new);
        register(PropertyImpl.Items.class, (WidgetFactory) ItemListEntry::new);

        initialized = true;
    }
}