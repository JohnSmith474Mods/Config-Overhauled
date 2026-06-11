# Config Overhauled

Welcome to Config Overhauled, a free to use multi-loader configuration library for Minecraft mod development! It provides a structured API for property definition, automated graphical interface generation, and network state synchronization for Fabric, Forge, or NeoForge environments.

The framework replaces manual interface construction and data synchronization with a declarative builder pattern. Properties are constrained by operational scopes (CLIENT, GLOBAL, LEVEL) that dictate data serialization targets and client-server synchronization authority. Built-in utilities handle dynamic GUI rendering and localization key export to eliminate structural boilerplate.

## Quickstart Guide 1.21.5

### Installation

Add the Modrinth Maven repository and the library dependency to build.gradle. Replace [VERSION] with the target release version.

```groovy
repositories {
    maven {
        name = "Modrinth"
        url = "https://api.modrinth.com/maven"
    }
}

dependencies {
    modImplementation "maven.modrinth:configoverhauled:[VERSION]"
}
```

### Configuration Definition

Initialize the manager, define structural boundaries, and register properties.

```java
package com.example.mod;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;

public class ExampleConfig {
    // Initializes the centralized configuration manager for the specified mod ID.
    public static final ConfigManager MANAGER = ConfigRegistry.getOrCreateManager("examplemod");

    // Defines a top-level classification category for the GUI and file structure.
    public static final Category EXAMPLE_CATEGORY = MANAGER.define("example_category");

    // Defines a structural subdivision within the category.
    public static final Group EXAMPLE_GROUP = EXAMPLE_CATEGORY.define("example_group");

    // Initiates the fluent builder sequence to construct and bind a new property.
    public static final Property<Boolean> ENABLE_FEATURE = EXAMPLE_GROUP.define("example_property")
            // Phase 1: Assigns the operational boundary and synchronization policy (CLIENT, GLOBAL, or LEVEL).
            .globalSide()
            // Phase 2: Binds the property to a fundamental data type, baseline state, and optional validation bounds.
            .asBoolean(true)
            // Phase 3: Injects a descriptive text sequence for disk serialization. (Optional)
            .withComment("An optional comment description for the .TOML file.")
            // Terminates the sequence, allocates the property instance, and binds it to the active ConfigManager.
            .register();
}
```

### Operational Scopes

Properties mandate an operational scope determining their storage location and network synchronization protocol:

- `CLIENT`: Operates exclusively within the local client environment. Serialized to the root config directory.

- `GLOBAL`: Operates universally across the environment instance. Serialized to the root config directory.

- `LEVEL`: Operates strictly within the boundary of a specific world instance. Serialized to the level save directory. The server maintains authoritative state and synchronizes properties to connected clients.

### Language File Generation

Config Overhauled dynamically constructs localization keys for client-side GUI translation.

Execute `/config_lang_gen <modid>` in-game to output a structured JSON file containing all required translation keys to the active configuration directory. Transfer these key-value pairs to the mod's `en_us.json` language file.

### Initialization & GUI Integration

The configuration manager requires path resolution during the primary boot sequence. Concurrently, the built-in screen factory must be registered to enable graphical interface generation.

#### Fabric
Path initialization executes within the primary mod entrypoint.
```java
package com.example.mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ExampleConfig.MANAGER.init(FabricLoader.getInstance().getConfigDir());
    }
}
```

GUI integration mandates a discrete [ModMenu](https://modrinth.com/mod/modmenu) implementation.
```java
package com.example.mod.client;

import com.example.mod.ExampleConfig;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ExampleConfig.MANAGER::createScreen;
    }
}
```

#### Forge
```java
package com.example.mod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(FMLJavaModLoadingContext context) {
        ExampleConfig.MANAGER.init(FMLPaths.CONFIGDIR.get());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) ->
                            ExampleConfig.MANAGER.createScreen(parentScreen)
                    )
            );
        }
    }
}
```

#### NeoForge
```java
package com.example.mod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(Constants.MOD_ID)
public class ExampleMod {
    public ExampleMod(ModContainer modContainer) {
        ExampleConfig.MANAGER.init(FMLPaths.CONFIGDIR.get());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parentScreen) ->
                    ExampleConfig.MANAGER.createScreen(parentScreen)
            );
        }
    }
}
```

## Credits

This project bundles [NightConfig](https://github.com/TheElectronWill/night-config) by [TheElectronWill](https://github.com/TheElectronWill), which is distributed under the GNU Lesser General Public License v3.0 (LGPL-3.0).

This project was created using [jaredlll08](https://modrinth.com/user/jaredlll08)'s [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).