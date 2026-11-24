# JadeTech

Jade addons for GregTech CEu Modern (Minecraft 1.20.1 / Forge) — extra Jade displays for the things GregTech's own Jade support does not show.

The first feature shows the **total amount of each fluid across a whole GregTech fluid pipe network**.

GregTech's fluid pipes each hold their own tank, and fluid keeps moving between them. GregTech's built-in Jade support only shows the tanks of the single pipe you are looking at, so the network-wide total is never visible anywhere. JadeTech fixes that: look at any fluid pipe and Jade adds one line per fluid with the amount summed over every connected pipe.

## What it does

- When you look at a GregTech fluid pipe, the server walks the connected pipe network (breadth-first, following each pipe's connection state, so wrench-disconnected neighbours are not crossed) and sums every pipe's tanks per fluid.
- Jade then shows lines like `Nitrogen total: 12,480 mB`, sorted by amount.
- Only the pipes themselves are counted. Tanks, drums and machines attached to the network are **not** included.
- Fluids are summed by fluid type only; NBT variants are merged.
- The walk is capped at 4,096 pipes as a safety net.

## Requirements

- Minecraft 1.20.1 / Forge 47+
- Jade 11.x (required)
- GregTech CEu Modern (the providers register only when it is loaded; without it the mod does nothing)

For multiplayer, install it on both the server (which computes the totals) and the client (which draws them). Singleplayer works with the one jar.

## Building

```sh
./gradlew build
```

Output: `build/libs/jadetech-0.1.0.jar`

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

## Known simplifications (v0.1.0)

- A face blocked by a cover still counts as connected for the walk if the pipe connection itself is open; pipe-to-pipe faces rarely carry covers, so this is ignored for now.
- Item pipes are not covered. In-transit items could be summed the same way in a future version.

## License

MIT
