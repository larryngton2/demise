package net.minecraft.tileentity;

import com.google.gson.JsonParseException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class TileEntitySign extends TileEntity {
    public final IChatComponent[] signText = new IChatComponent[]{new ChatComponentText(""), new ChatComponentText(""), new ChatComponentText(""), new ChatComponentText("")};
    public int lineBeingEdited = -1;
    private boolean isEditable = true;
    private EntityPlayer player;
    private final CommandResultStats stats = new CommandResultStats();

    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        for (int i = 0; i < 4; ++i) {
            String s = IChatComponent.Serializer.componentToJson(this.signText[i]);
            compound.setString("Text" + (i + 1), s);
        }

        this.stats.writeStatsToNBT(compound);
    }

    public void readFromNBT(NBTTagCompound compound) {
        this.isEditable = false;
        super.readFromNBT(compound);
        ICommandSender icommandsender = new ICommandSender() {
            public String getName() {
                return "Sign";
            }

            public IChatComponent getDisplayName() {
                return new ChatComponentText(this.getName());
            }

            public void addChatMessage(IChatComponent component) {
            }

            public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
                return true;
            }

            public BlockPos getPosition() {
                return TileEntitySign.this.pos;
            }

            public Vec3 getPositionVector() {
                return new Vec3((double) TileEntitySign.this.pos.getX() + 0.5D, (double) TileEntitySign.this.pos.getY() + 0.5D, (double) TileEntitySign.this.pos.getZ() + 0.5D);
            }

            public World getEntityWorld() {
                return TileEntitySign.this.worldObj;
            }

            public Entity getCommandSenderEntity() {
                return null;
            }

            public boolean sendCommandFeedback() {
                return false;
            }

            public void setCommandStat(CommandResultStats.Type type, int amount) {
            }
        };

        for (int i = 0; i < 4; ++i) {
            String s = compound.getString("Text" + (i + 1));

            try {
                IChatComponent ichatcomponent = IChatComponent.Serializer.jsonToComponent(s);

                try {
                    this.signText[i] = ChatComponentProcessor.processComponent(icommandsender, ichatcomponent, null);
                } catch (CommandException var7) {
                    this.signText[i] = ichatcomponent;
                }
            } catch (JsonParseException var8) {
                this.signText[i] = new ChatComponentText(s);
            }
        }

        this.stats.readStatsFromNBT(compound);
    }

    public Packet getDescriptionPacket() {
        IChatComponent[] aichatcomponent = new IChatComponent[4];
        System.arraycopy(this.signText, 0, aichatcomponent, 0, 4);
        return new S33PacketUpdateSign(this.worldObj, this.pos, aichatcomponent);
    }

    public boolean func_183000_F() {
        return true;
    }

    public boolean getIsEditable() {
        return this.isEditable;
    }

    public void setEditable(boolean isEditableIn) {
        this.isEditable = isEditableIn;

        if (!isEditableIn) {
            this.player = null;
        }
    }

    public void setPlayer(EntityPlayer playerIn) {
        this.player = playerIn;
    }

    public EntityPlayer getPlayer() {
        return this.player;
    }

    public boolean executeCommand(final EntityPlayer playerIn) {
        ICommandSender icommandsender = new ICommandSender() {
            public String getName() {
                return playerIn.getName();
            }

            public IChatComponent getDisplayName() {
                return playerIn.getDisplayName();
            }

            public void addChatMessage(IChatComponent component) {
            }

            public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
                return permLevel <= 2;
            }

            public BlockPos getPosition() {
                return TileEntitySign.this.pos;
            }

            public Vec3 getPositionVector() {
                return new Vec3((double) TileEntitySign.this.pos.getX() + 0.5D, (double) TileEntitySign.this.pos.getY() + 0.5D, (double) TileEntitySign.this.pos.getZ() + 0.5D);
            }

            public World getEntityWorld() {
                return playerIn.getEntityWorld();
            }

            public Entity getCommandSenderEntity() {
                return playerIn;
            }

            public boolean sendCommandFeedback() {
                return false;
            }

            public void setCommandStat(CommandResultStats.Type type, int amount) {
                TileEntitySign.this.stats.setCommandStatScore(this, type, amount);
            }
        };

        for (IChatComponent iChatComponent : this.signText) {
            ChatStyle chatstyle = iChatComponent == null ? null : iChatComponent.getChatStyle();

            if (chatstyle != null && chatstyle.getChatClickEvent() != null) {
                ClickEvent clickevent = chatstyle.getChatClickEvent();

                if (clickevent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    MinecraftServer.getServer().getCommandManager().executeCommand(icommandsender, clickevent.getValue());
                }
            }
        }

        return true;
    }

    public CommandResultStats getStats() {
        return this.stats;
    }
}
