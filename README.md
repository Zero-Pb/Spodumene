# JadeTech

Jade addons for GregTech CEu Modern (Minecraft 1.20.1 / Forge) — extra Jade displays for the things GregTech's own Jade support does not show.

The first feature shows the **total amount of each fluid across a whole GregTech fluid pipe network**.

GregTech's fluid pipes each hold their own tank, and fluid keeps moving between them. GregTech's built-in Jade support only shows the tanks of the single pipe you are looking at, so the network-wide total is never visible anywhere. JadeTech fixes that: look at any fluid pipe and Jade adds one line per fluid with the amount summed over every connected pipe.

## What it does

### Fluid pipe network totals
- When you look at a GregTech fluid pipe, the server walks the connected pipe network (breadth-first, following each pipe's connection state, so wrench-disconnected neighbours are not crossed) and sums every pipe's tanks per fluid.
- Jade then shows lines like `Nitrogen total: 12,480 / 64,000 mB`, sorted by amount, plus a line with the number of pipes in the network. Capacity is summed over the tanks currently holding each fluid.
- Only the pipes themselves are counted. Tanks, drums and machines attached to the network are **not** included.
- Fluids are summed by fluid type only; NBT variants are merged.
- The walk is capped at 4,096 pipes as a safety net.

### Multiblock hatch totals (v0.2.0)
- Look at a formed multiblock controller while sneaking (Jade's show-details key) and Jade lists the combined contents of all its fluid hatches and item buses, split into input and output: up to 8 fluids and 8 items per side, sorted by amount.
- GregTech's own Jade support only ever shows one machine at a time, so a multiblock's total contents are otherwise invisible without opening every hatch.

### Item pipe flow (v0.2.0)
- GregTech item pipes never hold items — transfers are simulated and each pipe only keeps a counter of items in transit, with no item types recorded. So instead of a contents list, looking at an item pipe shows one line with the total number of items currently in transit across the whole network: a quick way to tell whether a line is moving or stalled.

### Cable network summary (v0.2.0)
- GregTech's own Jade provider already shows a single cable's voltage and amperage, so JadeTech adds the network view instead: how many cables the network spans, its highest voltage tier, which machines are connected to it (with counts), a supply list of the machines currently outputting into the network with their live amperage (generators report their recipe-driven output, so this is what they are producing right now), and the total EU stored in its battery buffers.
- It also shows the maximum amperage the connected power sources could push (summed per voltage) next to the weakest cable's amperage limit, with a red warning when the supply could overload the cables. The endpoint list, supply list and battery total are details shown while sneaking; the headline and the warning are always visible.

### Configuration
- Every feature is a separate Jade provider, so each one can be switched off individually in Jade's own config screen.
- Display options live in `config/jadetech-client.toml`: fluid unit (mB or B), supply list style (machine names or amperage only), whether capacities / network counts / cable endpoints / battery EU / the flow warning are shown, how many entries multiblock lists show, and an option to always show the sneak-gated details without sneaking.

## Requirements

- Minecraft 1.20.1 / Forge 47+
- Jade 11.x (required)
- GregTech CEu Modern (the providers register only when it is loaded; without it the mod does nothing)

For multiplayer, install it on both the server (which computes the totals) and the client (which draws them). Singleplayer works with the one jar.

## Building

```sh
./gradlew build
```

Output: `build/libs/jadetech-0.2.0.jar`

Jade, GregTech CEu Modern and LDLib are pulled from CurseMaven at build time
(`curse.maven:jade-324717:5876199`, `curse.maven:gregtechceu-modern-890405:6764529`,
`curse.maven:ldlib-626676:8830518` for the LDLib interfaces GTCEu's pipe classes implement).
The code compiles against GTCEu 7.0.0; the GTCEu APIs it uses
(`FluidPipeBlockEntity#getFluidTanks`, `PipeBlockEntity#isConnected`)
are stable across the 1.20.1 line, including the 7.1.4 that
Monifactory 0.13.8 ships.

A manual-trigger GitHub Actions workflow is included under
`.github/workflows/build.yml` (same setup as Void Rain Fix: press
"Run workflow", get the jar from the `jadetech-jar` artifact).

## Known simplifications

- A face blocked by a cover still counts as connected for the walk if the pipe connection itself is open; pipe-to-pipe faces rarely carry covers, so this is ignored for now.
- Item pipe flow shows a total count only, because GregTech's item pipes do not record which items are in transit (see above).
- Multiblock totals cover fluid hatches and item buses. Dual input hatches are not included yet.

## License

MIT
