package johnsmith.configoverhauled.api.listener;

import java.util.List;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

import net.minecraft.world.item.Item;

public class ItemsPropertyListener extends AbstractPropertyListener<List<Item>> {
    protected ItemsPropertyListener(ConfigDescription description, List<Item> defaultValue) {
        super(description, DynamicPropertyTypeRegistry.ITEMS, defaultValue);
    }
}
