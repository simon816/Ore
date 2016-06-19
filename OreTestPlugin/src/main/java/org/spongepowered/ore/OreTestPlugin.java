package org.spongepowered.ore;

import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "ore-test",
        name = "Ore Test Plugin",
        version = "1.0.1",
        dependencies = {
                @Dependency(id = "ore-test", version = "1.0.0"),
                @Dependency(id = "worldedit", version = "1.0.0"),
                @Dependency(id = "bookotd", version = "1.0.0")
        },
        description = "Plugin for testing Ore functionality.",
        url = "https://ore-staging.spongepowered.org",
        authors = { "SpongePowered", "windy", "Zidane", "gabizou" }
)
public class OreTestPlugin {}
