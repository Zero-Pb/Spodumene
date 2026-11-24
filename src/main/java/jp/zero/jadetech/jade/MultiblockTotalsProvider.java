package jp.zero.jadetech.jade;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import jp.zero.jadetech.JadeTech;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Server side of the multiblock hatch totals display.
 *
 * <p>When a formed multiblock controller is looked at, this walks the
 * controller's parts, sums the contents of every fluid hatch and item bus
 * (split into input and output by each hatch's handler IO), and stores the
 * top entries in the Jade server data for {@link MultiblockTotalsComponent}
 * to draw. GregTech's own Jade support only ever shows one machine at a
 * time, so the combined contents of a multiblock are invisible without
 * opening every hatch.</p>
 *
 * <p>Fluids and items are summed by type only; NBT variants are merged,
 * matching the pipe totals behaviour.</p>
 */
public enum MultiblockTotalsProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    /** Max lines per category, so a huge multiblock cannot flood the tooltip. */
    private static final int MAX_ENTRIES = 8;

    public static final String FLUIDS_IN_KEY = "mb_fluids_in";
    public static final String FLUIDS_OUT_KEY = "mb_fluids_out";
    public static final String ITEMS_IN_KEY = "mb_items_in";
    public static final String ITEMS_OUT_KEY = "mb_items_out";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        // Machines are not block entities in this GregTech generation:
        // the block entity is a holder, so resolve the machine from the
        // position the way GregTech's own providers do.
        if (!(MetaMachine.getMachine(accessor.getLevel(), accessor.getPosition())
                instanceof MultiblockControllerMachine controller)) {
            return;
        }
        if (!controller.isFormed()) {
            return;
        }

        Map<Fluid, Long> fluidsIn = new HashMap<>();
        Map<Fluid, Long> fluidsOut = new HashMap<>();
        Map<Item, Long> itemsIn = new HashMap<>();
        Map<Item, Long> itemsOut = new HashMap<>();

        for (var part : controller.getParts()) {
            if (part instanceof FluidHatchPartMachine hatch) {
                Map<Fluid, Long> target = hatch.tank.getHandlerIO() == IO.OUT ? fluidsOut : fluidsIn;
                for (int i = 0; i < hatch.tank.getTanks(); i++) {
                    FluidStack stack = hatch.tank.getFluidInTank(i);
                    if (!stack.isEmpty() && stack.getAmount() > 0) {
                        target.merge(stack.getFluid(), (long) stack.getAmount(), Long::sum);
                    }
                }
            } else if (part instanceof ItemBusPartMachine bus) {
                Map<Item, Long> target = bus.getInventory().getHandlerIO() == IO.OUT ? itemsOut : itemsIn;
                var inventory = bus.getInventory();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        target.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
                    }
                }
            }
        }

        putFluids(data, FLUIDS_IN_KEY, fluidsIn);
        putFluids(data, FLUIDS_OUT_KEY, fluidsOut);
        putItems(data, ITEMS_IN_KEY, itemsIn);
        putItems(data, ITEMS_OUT_KEY, itemsOut);
    }

    private void putFluids(CompoundTag data, String key, Map<Fluid, Long> totals) {
        if (totals.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        totals.entrySet().stream()
                .sorted(Map.Entry.<Fluid, Long>comparingByValue().reversed())
                .limit(MAX_ENTRIES)
                .forEach(entry -> {
                    ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(entry.getKey());
                    if (fluidId == null) {
                        return;
                    }
                    CompoundTag tag = new CompoundTag();
                    tag.putString("fluid", fluidId.toString());
                    tag.putLong("amount", entry.getValue());
                    list.add(tag);
                });
        if (!list.isEmpty()) {
            data.put(key, list);
        }
    }

    private void putItems(CompoundTag data, String key, Map<Item, Long> totals) {
        if (totals.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        totals.entrySet().stream()
                .sorted(Map.Entry.<Item, Long>comparingByValue().reversed())
                .limit(MAX_ENTRIES)
                .forEach(entry -> {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getKey());
                    if (itemId == null) {
                        return;
                    }
                    CompoundTag tag = new CompoundTag();
                    tag.putString("item", itemId.toString());
                    tag.putLong("count", entry.getValue());
                    list.add(tag);
                });
        if (!list.isEmpty()) {
            data.put(key, list);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadeTech.id("multiblock_totals");
    }
}
