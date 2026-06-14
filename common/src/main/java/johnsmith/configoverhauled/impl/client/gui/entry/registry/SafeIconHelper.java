package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class SafeIconHelper {

    public static RenderableIcon getSafeIcon(Item item) {
        if (item == null || item == Items.AIR) {
            return (graphics, x, y) -> {};
        }

        try {
            ItemStack stack = item.getDefaultInstance();
            return (graphics, x, y) -> {
                graphics.nextStratum();
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(2.0F, 2.0F);
                graphics.item(stack, 0, 0);
                graphics.pose().popMatrix();
                graphics.nextStratum();
            };
        } catch (Exception e) {
            return (graphics, x, y) -> {};
        }
    }

    public static RenderableIcon getSafeIcon(Block block) {
        if (block == null || block.asItem() == Items.AIR) {
            return (graphics, x, y) -> {};
        }

        try {
            ItemStack stack = block.asItem().getDefaultInstance();
            return (graphics, x, y) -> {
                graphics.nextStratum();
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(2.0F, 2.0F);
                graphics.item(stack, 0, 0);
                graphics.pose().popMatrix();
                graphics.nextStratum();
            };
        } catch (Exception e) {
            return (graphics, x, y) -> {};
        }
    }
}

// Method for rendering the flat textures
// while in the main menu, deemed unviable:
//
// In a modded context, guessing the correct
// paths for each mod is unreliable at best.
/*
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SafeIconHelper {

    public static RenderableIcon getSafeIcon(Item item) {
        if (item == Items.AIR || item == null) return (graphics, x, y) -> {};

        if (Minecraft.getInstance().level != null) {
            try {
                ItemStack stack = item.getDefaultInstance();
                return (graphics, x, y) -> {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(x, y);
                    graphics.pose().scale(2.0F, 2.0F);
                    graphics.item(stack, 0, 0);
                    graphics.pose().popMatrix();
                };
            } catch (Exception ignored) {}
        }

        if (item instanceof BlockItem blockItem) {
            return getSafeIcon(blockItem.getBlock());
        }

        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        Identifier[] potentialPaths = new Identifier[]{
                Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/item/" + id.getPath() + ".png"),
                Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/block/" + id.getPath() + ".png")
        };

        for (Identifier textureId : potentialPaths) {
            if (Minecraft.getInstance().getResourceManager().getResource(textureId).isPresent()) {
                return (graphics, x, y) -> {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(x, y);
                    graphics.pose().scale(2.0F, 2.0F);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, 0, 0, 0.0f, 0.0f, 16, 16, 16, 16);
                    graphics.pose().popMatrix();
                };
            }
        }

        // Final fallback if absolutely nothing is found
        return (graphics, x, y) -> {};
    }

    public static RenderableIcon getSafeIcon(Block block) {
        if (block.asItem() == Items.AIR) return (graphics, x, y) -> {};

        if (Minecraft.getInstance().level != null) {
            try {
                ItemStack stack = block.asItem().getDefaultInstance();
                return (graphics, x, y) -> {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(x, y);
                    graphics.pose().scale(2.0F, 2.0F);
                    graphics.item(stack, 0, 0);
                    graphics.pose().popMatrix();
                };
            } catch (Exception ignored) {}
        }

        try {
            BlockState state = block.defaultBlockState();
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();

            String spritePath = sprite.contents().name().getPath();
            // Throw exception to force PNG fallback if sprite is missing or maps to dirt particles
            if (spritePath.equals("missingno") || spritePath.equals("block/dirt")) {
                throw new IllegalStateException("Force fallback");
            }

            return (graphics, x, y) -> {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(2.0F, 2.0F);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, 16, 16);
                graphics.pose().popMatrix();
            };
        } catch (Exception e) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            String namespace = id.getNamespace();
            String path = id.getPath();

            // Broaden the search criteria for complex blocks
            Identifier[] potentialPaths = new Identifier[]{
                    Identifier.fromNamespaceAndPath(namespace, "textures/block/" + path + ".png"),
                    Identifier.fromNamespaceAndPath(namespace, "textures/block/" + path + "_top.png"),
                    Identifier.fromNamespaceAndPath(namespace, "textures/block/" + path + "_side.png"),
                    Identifier.fromNamespaceAndPath(namespace, "textures/block/" + path + "_front.png"),
                    Identifier.fromNamespaceAndPath(namespace, "textures/item/" + path + ".png")
            };

            for (Identifier textureId : potentialPaths) {
                if (Minecraft.getInstance().getResourceManager().getResource(textureId).isPresent()) {
                    return (graphics, x, y) -> {
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(x, y);
                        graphics.pose().scale(2.0F, 2.0F);
                        graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, 0, 0, 0.0f, 0.0f, 16, 16, 16, 16);
                        graphics.pose().popMatrix();
                    };
                }
            }

            // Final fallback if absolutely nothing is found
            return (graphics, x, y) -> {};
        }
    }
}
*/