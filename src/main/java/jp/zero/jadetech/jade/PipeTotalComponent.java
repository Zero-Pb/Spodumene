package jp.zero.jadetech.jade;

import jp.zero.jadetech.JadeTech;
import jp.zero.jadetech.JadeTechConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client side of the network totals display.
 *
 * <p>Reads the totals that {@link PipeNetworkTotalsProvider} put into the
 * Jade server data and renders one line per fluid. Registered for every
 * block, but stays silent unless the data is present, which only happens
 * for GregTech fluid pipes.</p>
 *
 * <p>The totals themselves are always shown. The summed capacity and the
 * pipe count are details: they follow Jade's show-details key (sneak by
 * default), unless the config asks for details to always be visible.</p>
 */
public enum PipeTotalComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(PipeNetworkTotalsProvider.DATA_KEY)) {
            return;
        }
        boolean details = JadeTechConfig.detailsShown(accessor.showDetails());
        boolean showCapacity = details && JadeTechConfig.SHOW_CAPACITY.get();

        ListTag list = data.getList(PipeNetworkTotalsProvider.DATA_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation fluidId = new ResourceLocation(entry.getString("fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            Component fluidName = new FluidStack(fluid, 1).getDisplayName();
            String amount = FluidAmounts.format(entry.getLong("amount"));
            if (showCapacity) {
                tooltip.add(Component.translatable("jadetech.network_total_capacity",
                        fluidName, amount, FluidAmounts.format(entry.getLong("capacity"))));
            } else {
                tooltip.add(Component.translatable("jadetech.network_total", fluidName, amount));
            }
        }

        if (details && JadeTechConfig.SHOW_NETWORK_COUNTS.get()
                && data.contains(PipeNetworkTotalsProvider.PIPE_COUNT_KEY)) {
            int pipes = data.getInt(PipeNetworkTotalsProvider.PIPE_COUNT_KEY);
            tooltip.add(Component.translatable("jadetech.network_pipes", FluidAmounts.formatCount(pipes)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("pipe_network_totals");
    }
}
