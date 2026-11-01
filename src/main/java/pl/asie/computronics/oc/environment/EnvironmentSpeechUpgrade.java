package pl.asie.computronics.oc.environment;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class EnvironmentSpeechUpgrade extends AbstractManagedEnvironment {
    private final EnvironmentHost host;
    private float volume = 1.0f;
    private float basePitch = 1.0f;
    private final Queue<Character> phonemeQueue = new ArrayDeque<>();
    private int phonemeTimer = 0;

    public EnvironmentSpeechUpgrade(EnvironmentHost host) {
        this.host = host;
        setNode(Network.newNode(this, Visibility.Network)
                .withComponent("speech_box", Visibility.Neighbors)
                .withConnector(250)
                .create());
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!phonemeQueue.isEmpty()) {
            phonemeTimer++;
            if (phonemeTimer >= 2) {
                phonemeTimer = 0;
                char c = phonemeQueue.poll();
                if (!Character.isWhitespace(c)) {
                    playPhoneme(c);
                }
            }
        }
    }

    private void playPhoneme(char c) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        if (level == null || level.isClientSide) return;
        c = Character.toLowerCase(c);

        float charPitch = basePitch;
        if ("aeiouy".indexOf(c) >= 0) {
            charPitch = (float) (basePitch * (1.1 + (c % 5) * 0.15));
        } else if (Character.isLetter(c)) {
            charPitch = (float) (basePitch * (0.7 + (c % 7) * 0.08));
        }
        charPitch = Math.max(0.5f, Math.min(2.0f, charPitch));

        level.playSound(null, host.xPosition(), host.yPosition(), host.zPosition(),
                SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, volume * 1.5f, charPitch);

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.NOTE, host.xPosition(), host.yPosition() + 0.5, host.zPosition(), 1, (double) (charPitch * 10) / 24.0, 0, 0, 1.0);
        }
    }

    @Callback(doc = "function(text:string [, pitch:number]):boolean -- Speaks text aloud via robotic voice synthesizer.")
    public Object[] say(Context context, Arguments args) {
        Level level = host != null ? host.getEnvironmentLevel() : null;
        if (!(level instanceof ServerLevel sl)) return new Object[]{false, "world unavailable"};

        String text = args.checkString(0);
        if (text.length() > 256) text = text.substring(0, 256);
        if (args.count() >= 2) {
            this.basePitch = (float) Math.max(0.5, Math.min(args.checkDouble(1), 2.0));
        }

        this.phonemeQueue.clear();
        for (char c : text.toCharArray()) {
            this.phonemeQueue.add(c);
        }

        BlockPos hostPos = new BlockPos((int) host.xPosition(), (int) host.yPosition(), (int) host.zPosition());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(sl, new net.minecraft.world.level.ChunkPos(hostPos),
                new pl.asie.computronics.network.SpeechBoxPayload(hostPos, text, basePitch, volume));

        return new Object[]{true};
    }

    @Callback(doc = "function():boolean -- Stops speech.")
    public Object[] stop(Context context, Arguments args) {
        this.phonemeQueue.clear();
        return new Object[]{true};
    }

    @Callback(direct = true, doc = "function():boolean -- Checks if currently speaking.")
    public Object[] isProcessing(Context context, Arguments args) {
        return new Object[]{!phonemeQueue.isEmpty()};
    }

    @Callback(direct = true, doc = "function():number -- Gets pitch.")
    public Object[] getPitch(Context context, Arguments args) {
        return new Object[]{(double) basePitch};
    }

    @Callback(doc = "function(pitch:number):number -- Sets pitch (0.5 - 2.0).")
    public Object[] setPitch(Context context, Arguments args) {
        this.basePitch = (float) Math.max(0.5, Math.min(args.checkDouble(0), 2.0));
        return new Object[]{(double) basePitch};
    }
}
