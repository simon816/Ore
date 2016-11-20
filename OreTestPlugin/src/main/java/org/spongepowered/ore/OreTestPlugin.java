package org.spongepowered.ore;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "ore-test",
        name = "Ore Test Plugin",
        version = "1.0.0",
        dependencies = {
            @Dependency(id = "bookotd", version = "1.0.0"),
            @Dependency(id = "spongeapi", version = "4.0.0"),
            @Dependency(id = "Forge", version = "12.18.2.2151")
        },
        description = "Plugin for testing Ore functionality.",
        url = "https://ore-staging.spongepowered.org",
        authors = { "SpongePowered", "windy", "Zidane", "gabizou" }
)
public class OreTestPlugin {}
