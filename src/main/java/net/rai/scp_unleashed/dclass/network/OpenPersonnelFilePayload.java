package net.rai.scp_unleashed.dclass.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record OpenPersonnelFilePayload(boolean MainHand, byte Status, String Controller) implements CustomPacketPayload {
    public static final Type<OpenPersonnelFilePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_open_file"));
    public static final StreamCodec<ByteBuf, OpenPersonnelFilePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenPersonnelFilePayload::MainHand,
            ByteBufCodecs.BYTE, OpenPersonnelFilePayload::Status,
            ByteBufCodecs.STRING_UTF8, OpenPersonnelFilePayload::Controller,
            OpenPersonnelFilePayload::new);

    @Override
    public Type<OpenPersonnelFilePayload> type() {
        return PayloadType;
    }
}
