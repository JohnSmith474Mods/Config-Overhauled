package johnsmith.configoverhauled.api.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Defines the precise hierarchical coordinates required to locate, bind, or dynamically construct a configuration property.
 *
 * @param modId    The target namespace or mod identifier.
 * @param category The top-level classification category containing the target group.
 * @param group    The structural grouping containing the target property.
 * @param property The exact registry or resource name of the target property.
 */
public record ConfigDescription(
        String modId,
        String category,
        String group,
        String property
) {
    /**
     * Standard codec for network transmission and disk serialization of property coordinates.
     */
    public static final Codec<ConfigDescription> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("mod_id").forGetter(ConfigDescription::modId),
            Codec.STRING.fieldOf("category").forGetter(ConfigDescription::category),
            Codec.STRING.fieldOf("group").forGetter(ConfigDescription::group),
            Codec.STRING.fieldOf("property").forGetter(ConfigDescription::property)
    ).apply(instance, ConfigDescription::new));
}