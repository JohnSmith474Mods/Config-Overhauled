package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

import net.minecraft.world.item.Item;

public class ItemPropertyListener extends AbstractPropertyListener<Item> {
    protected ItemPropertyListener(ConfigDescription description, Item defaultValue) {
        super(description, DynamicPropertyTypeRegistry.ITEM, defaultValue);
    }
}
