package jp.zero.jadetech.jade;

import jp.zero.jadetech.JadeTech;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client side of the item pipe flow display. Stays silent unless the server
 * data carries an in-transit total from {@link ItemPipeFlowProvider}.
 */
public enum ItemPipeFlowComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(ItemPipeFlowProvider.TOTAL_KEY)) {
            return;
        }
        tooltip.add(Component.translatable("jadetech.item_flow",
                FluidAmounts.formatCount(data.getLong(ItemPipeFlowProvider.TOTAL_KEY))));
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("item_pipe_flow");
    }
}
