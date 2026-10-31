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

    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_REWIND =
            SOUNDS.register("tape_rewind", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "tape_rewind")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_EJECT =
            SOUNDS.register("tape_eject", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "tape_eject")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_INSERT =
            SOUNDS.register("tape_insert", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "tape_insert")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TAPE_BUTTON =
            SOUNDS.register("tape_button", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "tape_button")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TICKET_PRINT =
            SOUNDS.register("ticket_print", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "ticket_print")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON_PRESS =
            SOUNDS.register("button_press", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "button_press")));

    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON_RELEASE =
            SOUNDS.register("button_release", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Computronics.MODID, "button_release")));
}
