package io.github.zero_pb.spodumene;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Spodumene client configuration (config/spodumene-client.toml).
 *
 * <p>On/off toggles for whole features live in Jade's own config screen
 * (Jade lists every registered provider there). This file covers how the
 * displays look: units, which parts are shown, and whether the "details"
 * sections need the player to sneak (Jade's show-details key) or are
 * always visible.</p>
 */
public final class SpodumeneConfig {

    public enum FluidUnit {
        MB, B
    }

    public enum SupplyStyle {
        /** Machine name, amperage and tier: "Steam Turbine x2: 3 A (HV)". */
        FULL,
        /** Amperage and tier only, for players who find the names too long. */
        AMPS_ONLY
    }

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.EnumValue<FluidUnit> FLUID_UNIT;
    public static final ForgeConfigSpec.EnumValue<SupplyStyle> SUPPLY_STYLE;
    public static final ForgeConfigSpec.BooleanValue SHOW_CAPACITY;
    public static final ForgeConfigSpec.BooleanValue SHOW_NETWORK_COUNTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_CABLE_ENDPOINTS;
    public static final ForgeConfigSpec.BooleanValue SHOW_BATTERY_EU;
    public static final ForgeConfigSpec.BooleanValue SHOW_FLOW_WARNING;
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SHOW_DETAILS;
    public static final ForgeConfigSpec.IntValue MULTIBLOCK_MAX_ENTRIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("How Spodumene displays are rendered").push("display");
        FLUID_UNIT = builder
                .comment("Unit for fluid amounts: MB (millibuckets) or B (buckets)")
                .defineEnum("fluidUnit", FluidUnit.MB);
        SUPPLY_STYLE = builder
                .comment("Cable supply list style: FULL shows machine names, AMPS_ONLY shows just amperage and tier")
                .defineEnum("supplyStyle", SupplyStyle.FULL);
        SHOW_CAPACITY = builder
                .comment("Show the whole-network capacity line under fluid pipe totals (a detail, shown while sneaking)")
                .define("showCapacity", true);
        SHOW_NETWORK_COUNTS = builder
                .comment("Show how many pipes or cables a network has")
                .define("showNetworkCounts", true);
        SHOW_CABLE_ENDPOINTS = builder
                .comment("Show the list of machines connected to a cable network (a detail, shown while sneaking)")
                .define("showCableEndpoints", true);
        SHOW_BATTERY_EU = builder
                .comment("Show the total EU stored in a cable network's battery buffers (a detail, shown while sneaking)")
                .define("showBatteryEu", true);
        SHOW_FLOW_WARNING = builder
                .comment("Show the maximum amperage the connected power sources could push, and warn when it exceeds the weakest cable")
                .define("showFlowWarning", true);
        ALWAYS_SHOW_DETAILS = builder
                .comment("Always show the detail sections (capacities, hatch lists, cable endpoints and supply) without sneaking")
                .define("alwaysShowDetails", false);
        MULTIBLOCK_MAX_ENTRIES = builder
                .comment("Max fluid/item types shown per input/output list on multiblock controllers")
                .defineInRange("multiblockMaxEntries", 8, 1, 8);
        builder.pop();

        SPEC = builder.build();
    }

    private SpodumeneConfig() {
    }

    /** Details are shown while Jade's show-details key (sneak by default) is held, or always when configured. */
    public static boolean detailsShown(boolean showDetailsPressed) {
        return showDetailsPressed || ALWAYS_SHOW_DETAILS.get();
    }
}
