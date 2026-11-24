package jp.zero.spodumene.jade;

import net.minecraftforge.fml.ModList;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade plugin entry point.
 *
 * <p>The providers target GregTech's FluidPipeBlockEntity, so the actual
 * registration lives in {@link GtJadeRegistration} and only runs when
 * GregTech CEu Modern is loaded. Keeping the GT class references in a
 * separate class means this plugin class itself never touches GT types,
 * so the mod loads fine (and simply does nothing) without GregTech.</p>
 */
@WailaPlugin
public class SpodumeneJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        if (ModList.get().isLoaded("gtceu")) {
            GtJadeRegistration.registerServer(registration);
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (ModList.get().isLoaded("gtceu")) {
            GtJadeRegistration.registerClient(registration);
        }
    }
}
