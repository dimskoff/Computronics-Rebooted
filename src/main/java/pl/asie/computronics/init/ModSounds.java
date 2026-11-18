package pl.asie.computronics.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.asie.computronics.Computronics;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Computronics.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_REWIND = register("tape_rewind");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_EJECT = register("tape_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_INSERT = register("tape_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_BUTTON = register("tape_button");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON_PRESS = register("button_press");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON_RELEASE = register("button_release");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Computronics.MODID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
