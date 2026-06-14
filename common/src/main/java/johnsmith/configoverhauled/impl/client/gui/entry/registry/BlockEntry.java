package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class BlockEntry extends RegistryEntry<Block> {
    public BlockEntry(Property<Block> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(
                property,
                parentScreen,
                minecraft,
                onValueChanged,
                BuiltInRegistries.BLOCK,
                block -> {
                    Block boundBlock = BuiltInRegistries.BLOCK.get(BuiltInRegistries.BLOCK.getKey(block)).get().value();
                    return SafeIconHelper.getSafeIcon(boundBlock);
                },
                Block::getName
        );
    }
}