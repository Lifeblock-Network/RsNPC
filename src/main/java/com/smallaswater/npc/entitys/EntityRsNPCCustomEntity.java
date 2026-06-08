package com.smallaswater.npc.entitys;

import cn.nukkit.Player;
import cn.nukkit.entity.data.human.Skin;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.registry.Registries;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.variable.VariableManage;
import lombok.NonNull;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.ActorLinkType;
import org.cloudburstmc.protocol.bedrock.data.actor.ActorDataTypes;
import org.cloudburstmc.protocol.bedrock.data.actor.ActorLink;
import org.cloudburstmc.protocol.bedrock.packet.AddActorPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetActorLinkPacket;

/**
 * 基于自定义实体功能实现的RsNPC实体
 *
 * @author LT_Name
 */
public class EntityRsNPCCustomEntity extends EntityRsNPC {

    private String identifier;

    @Deprecated
    public EntityRsNPCCustomEntity(IChunk chunk, CompoundTag nbt) {
        this(chunk, nbt, null);
    }

    public EntityRsNPCCustomEntity(@NonNull IChunk chunk, @NonNull CompoundTag nbt, RsNpcConfig config) {
        super(chunk, nbt, config);
    }

    @Override
    public int getNetworkId() {
        return Registries.ENTITY.getEntityNetworkId(this.identifier);
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setSkinId(int skinId) {
        this.getNbt().putInt("skinId", skinId);
        this.setDataProperty(
                ActorDataTypes.SKIN_ID,
                this.getNbt().getInt("skinId")
        );
    }

    public int getSkinId() {
        return this.getNbt().getInt("skinId");
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDataProperty(
                ActorDataTypes.SKIN_ID,
                this.getNbt().getInt("skinId")
        );
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        this.level.addEntityMovement(this, x, y, z, yaw, pitch, headYaw);
    }

    @Override
    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.getUsedChunks().contains(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            player.sendPacket(createAddEntityPacket(player));
        }

        if (this.riding != null) {
            this.riding.spawnTo(player);

            SetActorLinkPacket pk = new SetActorLinkPacket();
            pk.setLink(new ActorLink(this.riding.getId(), this.getId(), ActorLinkType.RIDING, true, false, 0f));

            player.sendPacket(pk);
        }
    }

    @Override
    public BedrockPacket createAddEntityPacket() {
        AddActorPacket addEntity = new AddActorPacket();
        addEntity.setEntityType(this.getNetworkId());
        addEntity.setTargetActorID(this.getId());
        addEntity.setActorType(this.getIdentifier());
        addEntity.setTargetRuntimeID(this.getId());
        addEntity.setRotation(Vector2f.from((float) this.pitch, (float) this.yaw));
        addEntity.setHeadRotation((float) this.yaw);
        addEntity.setBodyRotation((float) this.yaw);
        addEntity.setPosition(Vector3f.from((float) this.x, (float) this.y + this.getBaseOffset(), (float) this.z));
        addEntity.setVelocity(Vector3f.from((float) this.motionX, (float) this.motionY, (float) this.motionZ));
        addEntity.setActorData(this.actorDataMap);

        for (int i = 0; i < this.passengers.size(); i++) {
            addEntity.getActorLinks().add(new ActorLink(this.getId(), this.passengers.get(i).getId(),
                    i == 0 ? ActorLinkType.RIDING : ActorLinkType.PASSENGER, false, false, 0f));
        }

        return addEntity;
    }

    public BedrockPacket createAddEntityPacket(Player player) {
        AddActorPacket pk = (AddActorPacket) this.createAddEntityPacket();
        pk.getActorData().putType(
                ActorDataTypes.NAME,
                VariableManage.stringReplace(player, this.getNameTag(), this.getConfig())
        );
        return pk;
    }

    @Override
    public void setSkin(Skin skin) {
        this.skin = skin;
    }

}
