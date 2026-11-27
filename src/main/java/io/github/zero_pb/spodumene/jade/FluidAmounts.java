package io.github.zero_pb.spodumene.jade;

import io.github.zero_pb.spodumene.SpodumeneConfig;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats fluid amounts for the Jade overlay, honouring the configured
 * unit (millibuckets or buckets). Client side only: reads the client
 * config, so server providers must never call this.
 */
public final class FluidAmounts {

    private static final NumberFormat INTEGERS = NumberFormat.getIntegerInstance(Locale.US);
    private static final DecimalFormat BUCKETS = new DecimalFormat("#,##0.##");

    private FluidAmounts() {
    }

    /** Formats an amount in millibuckets as e.g. "12,480 mB" or "12.48 B". */
    public static String format(long amountMb) {
        if (SpodumeneConfig.FLUID_UNIT.get() == SpodumeneConfig.FluidUnit.B) {
            if (amountMb % 1000 == 0) {
                return INTEGERS.format(amountMb / 1000) + " B";
            }
            return BUCKETS.format(amountMb / 1000.0) + " B";
        }
        return INTEGERS.format(amountMb) + " mB";
    }

    public static String formatCount(long value) {
        return INTEGERS.format(value);
    }
}
