package jp.zero.jadetech.jade;

import jp.zero.jadetech.JadeTech;
import jp.zero.jadetech.JadeTechConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client side of the multiblock hatch totals display.
 *
 * <p>Registered for every block, but stays silent unless the server data
 * carries the totals written by {@link MultiblockTotalsProvider}, which
 * only formed multiblock controllers ever produce. The lists are details:
 * they follow Jade's show-details key (sneak by default), unless the
 * config asks for details to always be visible.</p>
 */
public enum MultiblockTotalsComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null) {
            return;
        }
        if (!JadeTechConfig.detailsShown(accessor.showDetails())) {
            return;
        }
        addFluids(tooltip, data, MultiblockTotalsProvider.FLUIDS_IN_KEY, "jadetech.multiblock.fluids_in");
        addFluids(tooltip, data, MultiblockTotalsProvider.FLUIDS_OUT_KEY, "jadetech.multiblock.fluids_out");
        addItems(tooltip, data, MultiblockTotalsProvider.ITEMS_IN_KEY, "jadetech.multiblock.items_in");
        addItems(tooltip, data, MultiblockTotalsProvider.ITEMS_OUT_KEY, "jadetech.multiblock.items_out");
    }

    private void addFluids(ITooltip tooltip, CompoundTag data, String key, String headerKey) {
        if (!data.contains(key)) {
            return;
        }
        ListTag list = data.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(headerKey));
        int limit = Math.min(list.size(), JadeTechConfig.MULTIBLOCK_MAX_ENTRIES.get());
        for (int i = 0; i < limit; i++) {
            CompoundTag entry = list.getCompound(i);
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(entry.getString("fluid")));
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            Component fluidName = new FluidStack(fluid, 1).getDisplayName();
            tooltip.add(Component.literal("  ").append(Component.translatable(
                    "jadetech.multiblock.fluid", fluidName, FluidAmounts.format(entry.getLong("amount")))));
        }
    }

    private void addItems(ITooltip tooltip, CompoundTag data, String key, String headerKey) {
        if (!data.contains(key)) {
            return;
        }
        ListTag list = data.getList(key, Tag.TAG_COMPOUND);
        if (list.isEmpty()) {
            return;
        }
        tooltip.add(Component.translatable(headerKey));
        int limit = Math.min(list.size(), JadeTechConfig.MULTIBLOCK_MAX_ENTRIES.get());
        for (int i = 0; i < limit; i++) {
            CompoundTag entry = list.getCompound(i);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.getString("item")));
            if (item == null) {
                continue;
            }
            Component itemName = new ItemStack(item).getDisplayName();
            tooltip.add(Component.literal("  ").append(Component.translatable(
                    "jadetech.multiblock.item", itemName, FluidAmounts.formatCount(entry.getLong("count")))));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("multiblock_totals");
    }
}
