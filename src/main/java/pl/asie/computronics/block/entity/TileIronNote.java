package pl.asie.computronics.block.entity;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import pl.asie.computronics.init.ModBlockEntities;

import java.util.HashMap;
import java.util.Map;

public class TileIronNote extends TileEntityComputronics {
    private static final Map<String, Holder<SoundEvent>> INSTRUMENTS = new HashMap<>();
    private static final Holder<SoundEvent>[] INSTRUMENT_INDEX = new Holder[]{
            SoundEvents.NOTE_BLOCK_HARP,           // 0
            SoundEvents.NOTE_BLOCK_BASEDRUM,       // 1
            SoundEvents.NOTE_BLOCK_SNARE,          // 2
            SoundEvents.NOTE_BLOCK_HAT,            // 3
            SoundEvents.NOTE_BLOCK_BASS,           // 4
            SoundEvents.NOTE_BLOCK_FLUTE,          // 5
            SoundEvents.NOTE_BLOCK_BELL,           // 6
            SoundEvents.NOTE_BLOCK_GUITAR,         // 7
            SoundEvents.NOTE_BLOCK_CHIME,          // 8
            SoundEvents.NOTE_BLOCK_XYLOPHONE,      // 9
            SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, // 10
            SoundEvents.NOTE_BLOCK_COW_BELL,       // 11
            SoundEvents.NOTE_BLOCK_DIDGERIDOO,     // 12
            SoundEvents.NOTE_BLOCK_BIT,            // 13
            SoundEvents.NOTE_BLOCK_BANJO,          // 14
            SoundEvents.NOTE_BLOCK_PLING           // 15
    };

    static {
        INSTRUMENTS.put("harp", SoundEvents.NOTE_BLOCK_HARP);
        INSTRUMENTS.put("basedrum", SoundEvents.NOTE_BLOCK_BASEDRUM);
        INSTRUMENTS.put("bd", SoundEvents.NOTE_BLOCK_BASEDRUM);
        INSTRUMENTS.put("snare", SoundEvents.NOTE_BLOCK_SNARE);
        INSTRUMENTS.put("hat", SoundEvents.NOTE_BLOCK_HAT);
        INSTRUMENTS.put("bass", SoundEvents.NOTE_BLOCK_BASS);
        INSTRUMENTS.put("flute", SoundEvents.NOTE_BLOCK_FLUTE);
        INSTRUMENTS.put("bell", SoundEvents.NOTE_BLOCK_BELL);
        INSTRUMENTS.put("guitar", SoundEvents.NOTE_BLOCK_GUITAR);
        INSTRUMENTS.put("chime", SoundEvents.NOTE_BLOCK_CHIME);
        INSTRUMENTS.put("xylophone", SoundEvents.NOTE_BLOCK_XYLOPHONE);
        INSTRUMENTS.put("iron_xylophone", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE);
        INSTRUMENTS.put("cow_bell", SoundEvents.NOTE_BLOCK_COW_BELL);
        INSTRUMENTS.put("didgeridoo", SoundEvents.NOTE_BLOCK_DIDGERIDOO);
        INSTRUMENTS.put("bit", SoundEvents.NOTE_BLOCK_BIT);
        INSTRUMENTS.put("banjo", SoundEvents.NOTE_BLOCK_BANJO);
        INSTRUMENTS.put("pling", SoundEvents.NOTE_BLOCK_PLING);
    }

    public TileIronNote(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_NOTEBLOCK.get(), pos, state);
        node = Network.newNode(this, Visibility.Network)
                .withComponent("iron_noteblock", Visibility.Network)
                .create();
    }

    @Callback(direct = true, limit = 10, doc = "function([instrument:number or string,] note:number [, volume:number]):boolean -- Plays the specified note.")
    public Object[] playNote(Context context, Arguments args) {
        if (level == null) return new Object[]{false, "world unavailable"};

        Holder<SoundEvent> sound = SoundEvents.NOTE_BLOCK_HARP;
        int note = 0;
        float volume = 1.0f;

        if (args.count() >= 2 && !args.isDouble(0)) {
            // Case: playNote(instrument, note [, volume])
            if (args.isString(0)) {
                String inst = args.checkString(0).toLowerCase();
                sound = INSTRUMENTS.getOrDefault(inst, SoundEvents.NOTE_BLOCK_HARP);
            } else if (args.isInteger(0)) {
                int idx = Math.floorMod(args.checkInteger(0), INSTRUMENT_INDEX.length);
                sound = INSTRUMENT_INDEX[idx];
            }
            note = args.checkInteger(1);
            if (args.count() >= 3) {
                volume = (float) Math.max(0.0, Math.min(args.checkDouble(2), 1.0));
            }
        } else if (args.count() >= 2 && args.isDouble(0) && args.isDouble(1)) {
            // Case: playNote(note, volume)
            note = args.checkInteger(0);
            volume = (float) Math.max(0.0, Math.min(args.checkDouble(1), 1.0));
        } else if (args.count() >= 1) {
            // Case: playNote(note)
            note = args.checkInteger(0);
        }

        note = Math.max(0, Math.min(note, 24));
        float pitch = (float) Math.pow(2.0, (double) (note - 12) / 12.0);

        level.playSound(null, worldPosition, sound.value(), SoundSource.RECORDS, volume * 3.0f, pitch);

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.NOTE,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5,
                    1,
                    (double) note / 24.0, 0.0, 0.0, 1.0);
        }

        return new Object[]{true};
    }
}
