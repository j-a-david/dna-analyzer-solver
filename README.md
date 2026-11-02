# DNA Analyzer Solver

Standalone Fabric mod (client only, Minecraft 26.3) that solves the Hypixel SkyBlock
Garden DNA Analyzer puzzle. It highlights the two slots to swap next and recalculates
after each move. 

## What it does
- Detects a container whose title ends with " DNA" and whose 36 board slots are all DNA items.
- Solves for the minimum number of swaps with the same dynamic programming approach SkyHanni uses.
- Highlights the next two slots in green.


## Config
`config/dna-analyzer-solver.json` (created on first launch, restart to apply changes):
- `enabled`, `useMiddleClick`, `blockAccidentalClosing`, `hideTooltips`
- `allowEnds` (default false). SkyHanni fetches this flag from its online data repo and the
  jar does not contain the value. With false, the first and last strands are treated as fixed.
  If the puzzle lets you move them, set it to true.



## Credit and license
Solver logic and event wiring derived from SkyHanni (https://github.com/hannibal002/SkyHanni),
licensed MPL 2.0. This project is therefore also MPL 2.0, see LICENSE. Keep the file headers
and make your source available if you distribute the jar.
