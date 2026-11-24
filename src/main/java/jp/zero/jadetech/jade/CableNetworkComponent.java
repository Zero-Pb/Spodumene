package jp.zero.jadetech.jade;

import jp.zero.jadetech.JadeTech;
import jp.zero.jadetech.JadeTechConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client side of the cable network summary. Stays silent unless the server
 * data carries the summary written by {@link CableNetworkProvider}.
 *
 * <p>The headline and the max-flow warning are always shown. The endpoint
 * list, the supply list and the battery total are details: they follow
 * Jade's show-details key (sneak by default), unless the config asks for
 * details to always be visible.</p>
 */
public enum CableNetworkComponent implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(CableNetworkProvider.COUNT_KEY)) {
            return;
        }
        boolean details = JadeTechConfig.detailsShown(accessor.showDetails());

        if (JadeTechConfig.SHOW_NETWORK_COUNTS.get()) {
            tooltip.add(Component.translatable("jadetech.cable_network",
                    FluidAmounts.formatCount(data.getInt(CableNetworkProvider.COUNT_KEY)),
                    data.getString(CableNetworkProvider.MAX_TIER_KEY)));
        } else {
            tooltip.add(Component.translatable("jadetech.cable_network_no_count",
                    data.getString(CableNetworkProvider.MAX_TIER_KEY)));
        }

        if (JadeTechConfig.SHOW_FLOW_WARNING.get() && data.contains(CableNetworkProvider.MAX_SUPPLY_AMPS_KEY)) {
            long maxAmps = data.getLong(CableNetworkProvider.MAX_SUPPLY_AMPS_KEY);
            String tier = data.getString(CableNetworkProvider.MAX_SUPPLY_TIER_KEY);
            long limit = data.getLong(CableNetworkProvider.MIN_CABLE_AMPS_KEY);
            tooltip.add(Component.translatable("jadetech.cable_max_flow", maxAmps, tier, limit));
            if (maxAmps > limit) {
                tooltip.add(Component.translatable("jadetech.cable_overload_risk")
                        .withStyle(ChatFormatting.RED));
            }
        }

        if (!details) {
            return;
        }

        if (JadeTechConfig.SHOW_CABLE_ENDPOINTS.get() && data.contains(CableNetworkProvider.ENDPOINTS_KEY)) {
            ListTag list = data.getList(CableNetworkProvider.ENDPOINTS_KEY, Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                tooltip.add(Component.translatable("jadetech.cable_connected"));
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getString("block")));
                    if (block == null) {
                        continue;
                    }
                    tooltip.add(Component.literal("  ").append(Component.translatable(
                            "jadetech.cable_endpoint", block.getName(), entry.getInt("count"))));
                }
            }
        }

        if (data.contains(CableNetworkProvider.SUPPLY_KEY)) {
            ListTag list = data.getList(CableNetworkProvider.SUPPLY_KEY, Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                tooltip.add(Component.translatable("jadetech.cable_supply"));
                // Group identical sources (same block, amps and tier) into
                // one line with a count, e.g. "Steam Turbine x2: 3 A (HV)".
                Map<String, Integer> counts = new LinkedHashMap<>();
                Map<String, CompoundTag> representatives = new LinkedHashMap<>();
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag entry = list.getCompound(i);
                    String key = entry.getString("block") + "|" + entry.getLong("amps") + "|" + entry.getString("tier");
                    counts.merge(key, 1, Integer::sum);
                    representatives.putIfAbsent(key, entry);
                }
                boolean ampsOnly = JadeTechConfig.SUPPLY_STYLE.get() == JadeTechConfig.SupplyStyle.AMPS_ONLY;
                for (var counted : counts.entrySet()) {
                    CompoundTag entry = representatives.get(counted.getKey());
                    if (ampsOnly) {
                        // No machine names: "3 A (HV)" or "3 A (HV) x2".
                        Component line = Component.translatable("jadetech.cable_source_amps",
                                entry.getLong("amps"), entry.getString("tier"));
                        if (counted.getValue() > 1) {
                            line = Component.translatable("jadetech.cable_source_name_count", line, counted.getValue());
                        }
                        tooltip.add(Component.literal("  ").append(line));
                        continue;
                    }
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getString("block")));
                    if (block == null) {
                        continue;
                    }
                    Component name = counted.getValue() > 1
                            ? Component.translatable("jadetech.cable_source_name_count", block.getName(), counted.getValue())
                            : block.getName();
                    tooltip.add(Component.literal("  ").append(Component.translatable(
                            "jadetech.cable_source", name, entry.getLong("amps"), entry.getString("tier"))));
                }
            }
        }

        if (JadeTechConfig.SHOW_BATTERY_EU.get() && data.contains(CableNetworkProvider.BATTERY_CAP_KEY)) {
            tooltip.add(Component.translatable("jadetech.cable_battery",
                    FluidAmounts.formatCount(data.getLong(CableNetworkProvider.BATTERY_EU_KEY)),
                    FluidAmounts.formatCount(data.getLong(CableNetworkProvider.BATTERY_CAP_KEY))));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("cable_network");
    }
}
