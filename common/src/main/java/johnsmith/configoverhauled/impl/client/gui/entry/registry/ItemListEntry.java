package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ItemListEntry extends RegistryListEntry<Item> {
    public ItemListEntry(Property<List<Item>> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged, BuiltInRegistries.ITEM, ItemStack::new, Item::getDescription);
    }
}