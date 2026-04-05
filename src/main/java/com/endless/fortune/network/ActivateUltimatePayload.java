package com.endless.fortune.network;

import com.endless.fortune.EndlessFortune;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ActivateUltimatePayload() implements CustomPayload {

    public static final ActivateUltimatePayload INSTANCE = new ActivateUltimatePayload();
    public static final CustomPayload.Id<ActivateUltimatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(EndlessFortune.MOD_ID, "activate_ultimate"));
    public static final PacketCodec<RegistryByteBuf, ActivateUltimatePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
