package johnsmith.configoverhauled.api.listener;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;
import net.minecraft.world.level.block.Block;

public class BlockPropertyListener extends AbstractPropertyListener<Block> {
    protected BlockPropertyListener(ConfigDescription description, Block defaultValue) {
        super(description, DynamicPropertyTypeRegistry.BLOCK, defaultValue);
    }
}
