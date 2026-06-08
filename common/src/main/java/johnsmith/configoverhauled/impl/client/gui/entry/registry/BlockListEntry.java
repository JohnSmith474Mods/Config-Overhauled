package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.impl.client.gui.screen.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.util.List;

public class BlockListEntry extends RegistryListEntry<Block> {
    public BlockListEntry(Property<List<Block>> property, AbstractConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(property, parentScreen, minecraft, onValueChanged, BuiltInRegistries.BLOCK, block -> new ItemStack(block.asItem()), Block::getName);
    }
}