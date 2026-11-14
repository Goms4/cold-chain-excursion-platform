# ColdLatch design tokens

## Sampled from reference screenshots

Colors were sampled with Python Pillow from the four supplied Seal reference
screenshots (`codex-clipboard-26762e8e…png`, `fabe84b1…png`, `d7dc8a25…png`,
`79925427…png`). Browser chrome was excluded from the samples.

| Token | Sampled value | Evidence / use |
|---|---|---|
| Page / panel | `#FFFFFF` | Dominant background pixel in all four references |
| Selected row | `#F9F9F9` | Most frequent light grey selected row/card surface |
| Header strip | `#F5F5F5` | Sampled light grey interface strip |
| Hairline | `#D9D9D9` | Repeated grey divider sample |
| Problem red | `#AA1712` | Frequent red alert text sample in the checks panel |
| Pale red | `#FEF7F7` | Dominant pale-red sample in the conclusion block |
| Additional sampled red | `#E00606` | Observed in the screenshot; excluded from the interface so the UI uses one red accent |
| Ink | `#171717` | Dark near-black interface ink; screenshot text is antialiased |
| Secondary text | `#626262` | Mid-grey secondary text |

Typography is bundled locally as IBM Plex Mono with `ui-monospace` fallback.
All elements inherit this single family. Controls use 1px borders, 0–2px
radii, no shadows, and no gradients. The one accent is red, reserved for
failures, excursions, missing readings, flags, and final workflow markers.

Tokens live in the `:root` block in `frontend/src/index.css`; Tailwind colors
reference those variables and do not carry a second palette.

## Contrast check

Calculated from the sampled hex values using the WCAG relative-luminance
formula: `#171717` on white is 17.93:1, `#626262` on white is 6.10:1, and the
interface red `#AA1712` on white is 7.39:1. Each meets the 4.5:1 AA threshold
for normal text.
