package jp.zero.spodumene;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Spodumene.
 *
 * <p>GregTech's fluid pipes each hold their own tank, and fluid keeps
 * moving between them, so looking at a single pipe never tells you how
 * much fluid the whole network holds. GregTech's built-in Jade support
 * only shows the tanks of the one pipe you are looking at.</p>
 *
 * <p>This mod adds a Jade plugin that walks the connected fluid pipe
 * network (breadth-first, following the pipes' connection state),
 * sums up every pipe's tanks per fluid, and shows the totals in the
 * Jade overlay. Only the pipes themselves are counted; tanks and
 * machines attached to the network are not included.</p>
 */
@Mod(Spodumene.MOD_ID)
public class Spodumene {
    public static final String MOD_ID = "spodumene";
    private static final Logger LOGGER = LogManager.getLogger();

    public Spodumene() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SpodumeneConfig.SPEC);
        LOGGER.info("Spodumene: Jade addons for GregTech networks (pipe totals, multiblock hatches, item flow, cable networks)");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
