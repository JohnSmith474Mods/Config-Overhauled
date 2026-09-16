package johnsmith.configoverhauled.api.listener;

import java.util.List;

import johnsmith.configoverhauled.api.data.ConfigDescription;
import johnsmith.configoverhauled.api.registry.DynamicPropertyTypeRegistry;

import net.minecraft.world.level.block.Block;

public class BlocksPropertyListener extends AbstractPropertyListener<List<Block>> {
    protected BlocksPropertyListener(ConfigDescription description, List<Block> defaultValue) {
        super(description, DynamicPropertyTypeRegistry.BLOCKS, defaultValue);
    }
}
