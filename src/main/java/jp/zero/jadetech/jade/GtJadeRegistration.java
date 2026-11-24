package jp.zero.jadetech.jade;

import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;

/**
 * Registers the pipe-network providers with Jade.
 *
 * <p>This class references GregTech types, so it must only be loaded
 * after confirming GregTech is present (see {@link JadeTechJadePlugin}).</p>
 */
public final class GtJadeRegistration {

    private GtJadeRegistration() {
    }

    public static void registerServer(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PipeNetworkTotalsProvider.INSTANCE, FluidPipeBlockEntity.class);
    }

    public static void registerClient(IWailaClientRegistration registration) {
        // Registered for every block; the component only adds lines when
        // the server data carries our network totals, which only fluid
        // pipes ever produce.
        registration.registerBlockComponent(PipeTotalComponent.INSTANCE, Block.class);
    }
}
