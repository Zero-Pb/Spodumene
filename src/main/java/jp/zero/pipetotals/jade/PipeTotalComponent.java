package jp.zero.pipetotals.jade;

import jp.zero.pipetotals.PipeTotals;
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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Client side of the network totals display.
 *
 * <p>Reads the totals that {@link PipeNetworkTotalsProvider} put into the
 * Jade server data and renders one line per fluid. Registered for every
 * block, but stays silent unless the data is present, which only happens
 * for GregTech fluid pipes.</p>
 */
public enum PipeTotalComponent implements IBlockComponentProvider {
    INSTANCE;

    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(PipeNetworkTotalsProvider.DATA_KEY)) {
            return;
        }

        ListTag list = data.getList(PipeNetworkTotalsProvider.DATA_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation fluidId = new ResourceLocation(entry.getString("fluid"));
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            Component fluidName = new FluidStack(fluid, 1).getDisplayName();
            long amount = entry.getLong("amount");
            tooltip.add(Component.translatable("pipetotals.network_total", fluidName, FORMAT.format(amount)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return PipeTotals.id("pipe_network_totals");
    }
}
