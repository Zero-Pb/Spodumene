package jp.zero.spodumene.jade;

import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;
import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;
import com.gregtechceu.gtceu.common.blockentity.ItemPipeBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;

/**
 * Registers the Spodumene providers with Jade.
 *
 * <p>This class references GregTech types, so it must only be loaded
 * after confirming GregTech is present (see {@link SpodumeneJadePlugin}).</p>
 *
 * <p>Every feature has its own provider UID, which is also its toggle in
 * Jade's own config screen (Jade lists every registered provider there),
 * so users can switch individual Spodumene displays off without a
 * Spodumene-specific config file.</p>
 */
public final class GtJadeRegistration {

    private GtJadeRegistration() {
    }

    public static void registerServer(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PipeNetworkTotalsProvider.INSTANCE, FluidPipeBlockEntity.class);
        // Machines are not block entities in this GregTech generation,
        // so the multiblock provider registers for every block entity
        // (the same pattern GregTech's own machine providers use) and
        // resolves the controller itself.
        registration.registerBlockDataProvider(MultiblockTotalsProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(ItemPipeFlowProvider.INSTANCE, ItemPipeBlockEntity.class);
        registration.registerBlockDataProvider(CableNetworkProvider.INSTANCE, CableBlockEntity.class);
    }

    public static void registerClient(IWailaClientRegistration registration) {
        // Registered for every block; each component only adds lines when
        // the server data carries its feature's data, which only the
        // matching GregTech block entities ever produce.
        registration.registerBlockComponent(PipeTotalComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(MultiblockTotalsComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(ItemPipeFlowComponent.INSTANCE, Block.class);
        registration.registerBlockComponent(CableNetworkComponent.INSTANCE, Block.class);
    }
}
