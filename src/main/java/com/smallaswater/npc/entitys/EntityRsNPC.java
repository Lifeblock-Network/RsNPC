package com.smallaswater.npc.entitys;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.block.BlockLiquid;
import org.powernukkitx.entity.EntityHuman;
import org.powernukkitx.entity.custom.CustomEntity;
import org.powernukkitx.entity.custom.CustomEntityDefinition;
import org.powernukkitx.entity.data.human.Skin;
import org.powernukkitx.level.Level;
import org.powernukkitx.level.format.IChunk;
import org.powernukkitx.math.Vector3;
import org.powernukkitx.nbt.tag.CompoundTag;
import org.cloudburstmc.protocol.bedrock.data.ActorLinkType;
import org.cloudburstmc.protocol.bedrock.data.EmoteFlag;
import org.cloudburstmc.protocol.bedrock.data.actor.ActorDataMap;
import org.cloudburstmc.protocol.bedrock.data.actor.ActorDataTypes;
import org.cloudburstmc.protocol.bedrock.data.actor.ActorLink;
import org.cloudburstmc.protocol.bedrock.packet.EmotePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;
import org.cloudburstmc.protocol.bedrock.packet.RemoveActorPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetActorDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetActorLinkPacket;
import com.smallaswater.npc.RsNPC;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.route.Node;
import com.smallaswater.npc.route.RouteFinder;
import com.smallaswater.npc.variable.VariableManage;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashSet;
import java.util.LinkedList;

public class EntityRsNPC extends EntityHuman implements CustomEntity {

    /**
     * In PNX 3.0.0, {@code registerCustomEntity(Plugin, Class)} resolves the entity
     * definition from a static {@code definition()} method on the class; it no longer
     * accepts a runtime id passed at registration time.
     */
    public static CustomEntityDefinition definition() {
        return new CustomEntityDefinition("rsnpc:npc", "", false, true);
    }

    @Getter
    private final RsNpcConfig config;
    private int emoteSecond = 0;
    private int nextRouteIndex = 0;
    @Getter
    private final LinkedList<Node> nodes = new LinkedList<>();
    private Node nowNode;
    @Setter
    private boolean lockRoute = false;

    private Vector3 lastPos;
    private int lastUpdateNodeTick;

    private RouteFinder nowRouteFinder;
    @Setter
    private int pauseMoveTick = 0;

    /**
     * An RsNPC entity must be created with an RsNpcConfig argument; this constructor is kept only for compatibility with the core's entity-creation method
     */
    @Deprecated
    public EntityRsNPC(IChunk chunk, CompoundTag nbt) {
        this(chunk, nbt, null);
    }

    public EntityRsNPC(@NonNull IChunk chunk, @NonNull CompoundTag nbt, RsNpcConfig config) {
        super(chunk, nbt);
        this.config = config;
        if (this.config == null) {
            this.close();
            return;
        }
        this.setNameTagAlwaysVisible(config.isNameTagAlwaysVisible());
        this.setNameTagVisible(true);
        this.setNameTag(config.getShowName());
        this.setHealthMax(20);
        this.setHealthCurrent(20.0F);
        this.getInventory().setItemInMainHand(config.getHand());
        this.getInventory().setArmorContents(config.getArmor());

        //the following runs in initEntity(); it needs to run again once the config is available
        if (config.isEnableCustomCollisionSize()) {
            this.actorDataMap.put(ActorDataTypes.HEIGHT, this.getHeight());
            this.actorDataMap.put(ActorDataTypes.WIDTH, this.getWidth());
            this.actorDataMap.put(ActorDataTypes.STRUCTURAL_INTEGRITY, (int) this.getHealthCurrent());
        }
    }

    @Override
    public float getWidth() {
        if (this.config != null && this.config.isEnableCustomCollisionSize()) {
            return this.config.getCustomCollisionSizeWidth();
        }
        return super.getWidth();
    }

    @Override
    public float getLength() {
        if (this.config != null && this.config.isEnableCustomCollisionSize()) {
            return this.config.getCustomCollisionSizeLength();
        }
        return super.getLength();
    }

    @Override
    public float getHeight() {
        if (this.config != null && this.config.isEnableCustomCollisionSize()) {
            return this.config.getCustomCollisionSizeHeight();
        }
        return super.getHeight();
    }

    @Override
    public int getNetworkId() {
        if (this.config == null) {
            return super.getNetworkId();
        }
        return this.config.getNetworkId();
    }

    @Override
    protected float getBaseOffset() {
        if (this.getNetworkId() == -1) {
            return super.getBaseOffset();
        }
        return 0.0F;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.config == null) {
            this.close();
            return false;
        }

        //rotate
        if (this.config.getWhirling() != 0) {
            this.yaw += this.config.getWhirling();
        } else {
            //pathfinding
            if (!this.config.getRoute().isEmpty() && this.pauseMoveTick <= 0) {
                this.processMove(currentTick);
            } else {
                //lookPlayer
                if (currentTick % 2 == 0 && this.config.isLookAtThePlayer() && !this.getViewers().isEmpty()) {
                    this.seePlayer();
                }

                if (this.pauseMoveTick > 0) {
                    this.pauseMoveTick--;
                }

                //emote
                if (this.config.isEnableEmote() && !this.config.getEmoteIDs().isEmpty()) {
                    if (currentTick % 20 == 0) {
                        this.emoteSecond++;
                    }
                    if (this.emoteSecond >= this.config.getShowEmoteInterval()) {
                        this.emoteSecond = 0;
                        EmotePacket packet = new EmotePacket();
                        packet.setActorRuntimeId(this.getId());
                        packet.setEmoteId(this.config.getEmoteIDs().get(RsNPC.RANDOM.nextInt(this.config.getEmoteIDs().size())));
                        packet.getFlags().add(EmoteFlag.SERVER_SIDE); // FLAG_SERVER | FLAG_MUTE_ANNOUNCEMENT
                        packet.getFlags().add(EmoteFlag.MUTE_EMOTE_CHAT);
                        packet.setXuid("");
                        packet.setPlatformId("");
                        Server.broadcastPacket(this.getViewers().values(), packet);
                    }
                }
            }
        }

        return super.onUpdate(currentTick);
    }

    private void processMove(int currentTick) {
        if (this.nodes.isEmpty()) {
            Vector3 next = this.config.getRoute().get(this.nextRouteIndex);
            if (this.config.isEnablePathfinding()) {
                if (!this.lockRoute) {
                    this.setLockRoute(true);
                    this.nextRouteIndex++;
                    if (this.nextRouteIndex >= this.config.getRoute().size()) {
                        this.nextRouteIndex = 0;
                    }
                    this.nowRouteFinder = new RouteFinder(this.getLevel(), this, next);
                } else if (this.nowRouteFinder != null && this.nowRouteFinder.isProcessingComplete()) {
                    this.nodes.addAll(this.nowRouteFinder.getNodes());
                    this.setLockRoute(false);
                }
            } else {
                this.nodes.add(new Node(next));
                this.nextRouteIndex++;
                if (this.nextRouteIndex >= this.config.getRoute().size()) {
                    this.nextRouteIndex = 0;
                }
            }
        }

        if (!this.nodes.isEmpty()) {
            if (this.nowNode == null || this.distance(this.nowNode.getVector3()) <= 0.35/*((this.getWidth()) / 2 + 0.05)*/) {
                this.nowNode = this.nodes.poll();
                this.lastUpdateNodeTick = currentTick;
            }
            if (this.nowNode != null) {
                Vector3 vector3 = this.nowNode.getVector3();

                if (currentTick - this.lastUpdateNodeTick > 100) {
                    if (this.distance(lastPos) < 0.1) {
                        this.setPosition(vector3);
                        return;
                    }
                    this.lastUpdateNodeTick = currentTick;
                }

                this.lastPos = this.getLocation();
                double x = vector3.x - this.x;
                double z = vector3.z - this.z;
                double diff = Math.abs(x) + Math.abs(z);
                this.motionY = this.config.getBaseMoveSpeed() * vector3.y - this.y;
                if (this.getLevelBlock() instanceof BlockLiquid) {
                    this.motionX = this.config.getBaseMoveSpeed() * 0.05 * (x / diff);
                    this.motionZ = this.config.getBaseMoveSpeed() * 0.05 * (z / diff);
                } else {
                    this.motionX = this.config.getBaseMoveSpeed() * 0.15 * (x / diff);
                    this.motionZ = this.config.getBaseMoveSpeed() * 0.15 * (z / diff);
                }
                this.move(this.motionX, this.motionY, this.motionZ);

                //view angle calculation
                if (currentTick % 4 == 0) {
                    if (this.nodes.size() >= 2) {
                        vector3 = this.nodes.get(1).getVector3();
                    }
                    double dx = this.x - vector3.x;
                    double dz = this.z - vector3.z;
                    double yaw = Math.asin(dx / Math.sqrt(dx * dx + dz * dz)) / Math.PI * 180.0D;
                    if (dz > 0.0D) {
                        yaw = -yaw + 180.0D;
                    }
                    this.yaw = yaw;
                    this.headYaw = yaw;
                    this.pitch = 0;
                }
            }
        }
    }

    private void seePlayer() {
        RsNPC.THREAD_POOL_EXECUTOR.execute(() -> {
            LinkedList<Player> npd = new LinkedList<>(this.getViewers().values());
            npd.sort((p1, p2) -> Double.compare(this.distance(p1) - this.distance(p2), 0.0D));
            Player player = npd.poll();
            if (player != null) {
                double dx = this.x - player.x;
                double dy = this.y - player.y;
                double dz = this.z - player.z;
                double yaw = Math.asin(dx / Math.sqrt(dx * dx + dz * dz)) / Math.PI * 180.0D;
                double pitch = Math.round(Math.asin(dy / Math.sqrt(dx * dx + dz * dz + dy * dy)) / Math.PI * 180.0D);
                if (dz > 0.0D) {
                    yaw = -yaw + 180.0D;
                }
                this.yaw = yaw;
                this.headYaw = yaw;
                this.pitch = pitch;
            }
        });
    }

    @Override
    public void addMovement(double x, double y, double z, double yaw, double pitch, double headYaw) {
        if (this.getNetworkId() == -1) {
            this.level.addPlayerMovement(this, x, y, z, yaw, pitch, headYaw);
        } else {
            this.level.addEntityMovement(this, x, y, z, yaw, pitch, headYaw);
        }
    }

    @Override
    public void spawnTo(Player player) {
        if (this.getNetworkId() == -1) {
            super.spawnTo(player);
            this.sendData(player);
        }

        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.getUsedChunks().contains(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            player.sendPacket(this.createAddEntityPacket());
            this.sendData(player);
        }
        if (this.riding != null) {
            this.riding.spawnTo(player);
            SetActorLinkPacket pkk = new SetActorLinkPacket();
            pkk.setLink(new ActorLink(this.riding.getId(), this.getId(), ActorLinkType.RIDING, true, false, 0f));
            player.sendPacket(pkk);
        }
    }

    @Override
    public void despawnFrom(Player player) {
        if (this.getNetworkId() == -1) {
            super.despawnFrom(player);
        }

        if (this.hasSpawned.containsKey(player.getLoaderId())) {
            RemoveActorPacket pk = new RemoveActorPacket();
            pk.setTargetActorID(this.getId());
            player.sendPacket(pk);
            this.hasSpawned.remove(player.getLoaderId());
        }
    }

    @Override
    public void setSkin(Skin skin) {
        Skin oldSkin = this.getSkin();
        super.setSkin(skin);
        this.sendSkin(oldSkin);
    }

    protected void sendSkin(Skin oldSkin) {
        PlayerSkinPacket packet = new PlayerSkinPacket();
        packet.setSerializedSkin(this.getSkin().getSkin());
        packet.setNewSkinName(this.getSkin().getSkin().getSkinId());
        packet.setOldSkinName(oldSkin != null ? oldSkin.getSkin().getSkinId() : "old");
        packet.setUuid(this.getUniqueId());
        HashSet<Player> players = new HashSet<>(this.getViewers().values());
        if (!players.isEmpty()) {
            Server.broadcastPacket(players, packet);
        }
    }

    @Override
    public void sendData(Player player, ActorDataMap data) {
        SetActorDataPacket pk = new SetActorDataPacket();
        pk.setTargetRuntimeID(this.getId());
        ActorDataMap entityData = data == null ? this.actorDataMap : data;
        entityData.putType(
                ActorDataTypes.NAME,
                VariableManage.stringReplace(player, this.getNameTag(), this.getConfig())
        );
        pk.setActorData(entityData);
        player.sendPacket(pk);
    }

    @Override
    public void sendData(Player[] players, ActorDataMap data) {
        for (Player player : players) {
            SetActorDataPacket pk = new SetActorDataPacket();
            pk.setTargetRuntimeID(this.getId());
            ActorDataMap entityData = data == null ? this.actorDataMap : data;
            entityData.putType(
                    ActorDataTypes.NAME,
                    VariableManage.stringReplace(player, this.getNameTag(), this.getConfig())
            );
            pk.setActorData(entityData);
            player.sendPacket(pk);
        }
    }
}