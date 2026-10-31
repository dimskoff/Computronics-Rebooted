# Particle FX Card

Expansion card that emits programmable modern particle effects around host.

## Component Name
`particle_fx`

## Methods
* `spawn(type: string, [count: number], [offsetX: number, offsetY: number, offsetZ: number], [speed: number]): boolean` - Emits particles.
* `getTypes(): table` - Returns list of all supported particle types.
* `getParticleLimit(): number` - Maximum particles per tick (100).
