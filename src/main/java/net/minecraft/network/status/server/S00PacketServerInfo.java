package net.minecraft.network.status.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.IChatComponent;

public class S00PacketServerInfo implements Packet<INetHandlerStatusClient> {
    private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(ServerStatusResponse.MinecraftProtocolVersionIdentifier.class, new ServerStatusResponse.MinecraftProtocolVersionIdentifier.Serializer()).registerTypeAdapter(ServerStatusResponse.PlayerCountData.class, new ServerStatusResponse.PlayerCountData.Serializer()).registerTypeAdapter(ServerStatusResponse.class, new ServerStatusResponse.Serializer()).registerTypeHierarchyAdapter(IChatComponent.class, new IChatComponent.Serializer()).registerTypeHierarchyAdapter(ChatStyle.class, new ChatStyle.Serializer()).registerTypeAdapterFactory(new EnumTypeAdapterFactory()).create();
    private ServerStatusResponse response;

    public S00PacketServerInfo() {
    }

    public S00PacketServerInfo(ServerStatusResponse responseIn) {
        this.response = responseIn;
    }

    public void readPacketData(PacketBuffer buf) {
        this.response = GSON.fromJson(buf.readStringFromBuffer(32767), ServerStatusResponse.class);
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeString(GSON.toJson(this.response));
    }

    public void processPacket(INetHandlerStatusClient handler) {
        handler.handleServerInfo(this);
    }

    public ServerStatusResponse getResponse() {
        return this.response;
    }
}
