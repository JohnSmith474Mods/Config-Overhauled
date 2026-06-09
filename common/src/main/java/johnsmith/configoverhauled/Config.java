package johnsmith.configoverhauled;

import com.mojang.serialization.Codec;
import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

import johnsmith.configoverhauled.impl.platform.Services;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class Config {
    public static final ConfigManager MANAGER = ConfigRegistry.getOrCreateManager(Constants.MOD_ID);

    public static final Category CATEGORY_CLIENT = MANAGER.define("client_settings");

    public static final Group GROUP_ACCESSIBILITY = CATEGORY_CLIENT.define("accessibility");
    public static final Group GROUP_ALERT = CATEGORY_CLIENT.define("alert");

    public enum TestMode {
        STANDARD,
        ADVANCED,
        EXPERT
    }

    public static final Property<Integer> DATA_GEN_LINK_COLOR = GROUP_ACCESSIBILITY.define("data_gen_link_color")
            .clientSide()
            .asColor(0x00FFFF)
            .withComment("Specifies the default visualization color for generated data links.")
            .register();

    public static final Property<Boolean> SHOW_RELOAD_ALERT = GROUP_ALERT.define("show_reload_alert")
            .clientSide()
            .asBoolean(true)
            .withComment("Specifies whether to show a /reload alert for leaving the config screen after editing a LEVEL scoped config.")
            .register();

    static {
        //if (Services.PLATFORM.isDevelopmentEnvironment()) {
            registerTestConfigs();
        //}
    }

    private static void registerTestConfigs() {
        Category CATEGORY_CLIENT_TEST = MANAGER.define("client_test");
        Category CATEGORY_GLOBAL_TEST = MANAGER.define("global_test");
        Category CATEGORY_LEVEL_TEST = MANAGER.define("level_test");

        Group GROUP_CLIENT_TEST = CATEGORY_CLIENT_TEST.define("test");
        Group GROUP_GLOBAL_TEST = CATEGORY_GLOBAL_TEST.define("test");
        Group GROUP_LEVEL_TEST = CATEGORY_LEVEL_TEST.define("test");

        GROUP_CLIENT_TEST.define("boolean_test")
                .clientSide()
                .asBoolean(true)
                .withComment("A test boolean client property.")
                .register();
        GROUP_CLIENT_TEST.define("integer_test")
                .clientSide()
                .asInteger(0)
                .withComment("A test integer client property.")
                .register();
        GROUP_CLIENT_TEST.define("long_test")
                .clientSide()
                .asLong(0)
                .withComment("A test long client property.")
                .register();
        GROUP_CLIENT_TEST.define("color_test")
                .clientSide()
                .asColor(0xC0FFEE)
                .withComment("A test color client property.")
                .register();
        GROUP_CLIENT_TEST.define("float_test")
                .clientSide()
                .asFloat(0.0F)
                .withComment("A test float client property.")
                .register();
        GROUP_CLIENT_TEST.define("double_test")
                .clientSide()
                .asDouble(0.0D)
                .withComment("A test double client property.")
                .register();
        GROUP_CLIENT_TEST.define("string_test")
                .clientSide()
                .asString("default_value")
                .withComment("A test string client property.")
                .register();
        GROUP_CLIENT_TEST.define("enum_test")
                .clientSide()
                .asEnum(TestMode.STANDARD, Codec.STRING.xmap(TestMode::valueOf, Enum::name))
                .withComment("A test enum client property.")
                .register();
        GROUP_CLIENT_TEST.define("list_test")
                .clientSide()
                .asList(List.of("entry1", "entry2", "entry3"), Codec.STRING)
                .withComment("A test list client property.")
                .register();
        GROUP_CLIENT_TEST.define("block_test")
                .clientSide()
                .asBlock(Blocks.GRASS_BLOCK)
                .withComment("A test block client property.")
                .register();
        GROUP_CLIENT_TEST.define("item_test")
                .clientSide()
                .asItem(Items.DIAMOND_PICKAXE)
                .withComment("A test item client property.")
                .register();
        GROUP_CLIENT_TEST.define("blocks_test")
                .clientSide()
                .asBlocks(List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE))
                .withComment("A test blocks client property.")
                .register();
        GROUP_CLIENT_TEST.define("items_test")
                .clientSide()
                .asItems(List.of(Items.DIAMOND_PICKAXE, Items.ENCHANTED_GOLDEN_APPLE))
                .withComment("A test items client property.")
                .register();

        GROUP_GLOBAL_TEST.define("boolean_test")
                .globalSide()
                .asBoolean(true)
                .withComment("A test boolean global property.")
                .register();
        GROUP_GLOBAL_TEST.define("integer_test")
                .globalSide()
                .asInteger(0)
                .withComment("A test integer global property.")
                .register();
        GROUP_GLOBAL_TEST.define("long_test")
                .globalSide()
                .asLong(0)
                .withComment("A test long global property.")
                .register();
        GROUP_GLOBAL_TEST.define("color_test")
                .globalSide()
                .asColor(0xC0FFEE)
                .withComment("A test color global property.")
                .register();
        GROUP_GLOBAL_TEST.define("float_test")
                .globalSide()
                .asFloat(0.0F)
                .withComment("A test float global property.")
                .register();
        GROUP_GLOBAL_TEST.define("double_test")
                .globalSide()
                .asDouble(0.0D)
                .withComment("A test double global property.")
                .register();
        GROUP_GLOBAL_TEST.define("string_test")
                .globalSide()
                .asString("default_value")
                .withComment("A test string global property.")
                .register();
        GROUP_GLOBAL_TEST.define("enum_test")
                .globalSide()
                .asEnum(TestMode.STANDARD, Codec.STRING.xmap(TestMode::valueOf, Enum::name))
                .withComment("A test enum global property.")
                .register();
        GROUP_GLOBAL_TEST.define("list_test")
                .globalSide()
                .asList(List.of("entry1", "entry2", "entry3"), Codec.STRING)
                .withComment("A test list global property.")
                .register();
        GROUP_GLOBAL_TEST.define("block_test")
                .globalSide()
                .asBlock(Blocks.GRASS_BLOCK)
                .withComment("A test block global property.")
                .register();
        GROUP_GLOBAL_TEST.define("item_test")
                .globalSide()
                .asItem(Items.DIAMOND_PICKAXE)
                .withComment("A test item global property.")
                .register();
        GROUP_GLOBAL_TEST.define("blocks_test")
                .globalSide()
                .asBlocks(List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE))
                .withComment("A test blocks global property.")
                .register();
        GROUP_GLOBAL_TEST.define("items_test")
                .globalSide()
                .asItems(List.of(Items.DIAMOND_PICKAXE, Items.ENCHANTED_GOLDEN_APPLE))
                .withComment("A test items global property.")
                .register();

        GROUP_LEVEL_TEST.define("boolean_test")
                .levelSide()
                .asBoolean(true)
                .withComment("A test boolean level property.")
                .register();
        GROUP_LEVEL_TEST.define("integer_test")
                .levelSide()
                .asInteger(0)
                .withComment("A test integer level property.")
                .register();
        GROUP_LEVEL_TEST.define("long_test")
                .levelSide()
                .asLong(0)
                .withComment("A test long level property.")
                .register();
        GROUP_LEVEL_TEST.define("color_test")
                .levelSide()
                .asColor(0xC0FFEE)
                .withComment("A test color level property.")
                .register();
        GROUP_LEVEL_TEST.define("float_test")
                .levelSide()
                .asFloat(0.0F)
                .withComment("A test float level property.")
                .register();
        GROUP_LEVEL_TEST.define("double_test")
                .levelSide()
                .asDouble(0.0D)
                .withComment("A test double level property.")
                .register();
        GROUP_LEVEL_TEST.define("string_test")
                .levelSide()
                .asString("default_value")
                .withComment("A test string level property.")
                .register();
        GROUP_LEVEL_TEST.define("enum_test")
                .levelSide()
                .asEnum(TestMode.STANDARD, Codec.STRING.xmap(TestMode::valueOf, Enum::name))
                .withComment("A test enum level property.")
                .register();
        GROUP_LEVEL_TEST.define("list_test")
                .levelSide()
                .asList(List.of("entry1", "entry2", "entry3"), Codec.STRING)
                .withComment("A test list level property.")
                .register();
        GROUP_LEVEL_TEST.define("block_test")
                .levelSide()
                .asBlock(Blocks.GRASS_BLOCK)
                .withComment("A test block level property.")
                .register();
        GROUP_LEVEL_TEST.define("item_test")
                .levelSide()
                .asItem(Items.DIAMOND_PICKAXE)
                .withComment("A test item level property.")
                .register();
        GROUP_LEVEL_TEST.define("blocks_test")
                .levelSide()
                .asBlocks(List.of(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.STONE))
                .withComment("A test blocks level property.")
                .register();
        GROUP_LEVEL_TEST.define("items_test")
                .levelSide()
                .asItems(List.of(Items.DIAMOND_PICKAXE, Items.ENCHANTED_GOLDEN_APPLE))
                .withComment("A test items level property.")
                .register();
    }
}