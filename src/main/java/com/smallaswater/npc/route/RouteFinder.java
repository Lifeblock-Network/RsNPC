package com.smallaswater.npc.route;

import cn.nukkit.Server;
import cn.nukkit.block.*;
import cn.nukkit.level.Level;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.IChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.AsyncTask;
import com.smallaswater.npc.RsNPC;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author lt_name
 */
@Getter
public class RouteFinder {

    private final int startTick;

    private boolean processingComplete = false;

    private final Level level;
    private final Vector3 start;
    private final Vector3 end;
    private final int distance;

    LinkedList<Node> openNodes = new LinkedList<>();
    ArrayList<Vector3> closeNodes = new ArrayList<>();

    LinkedList<Node> nodes = new LinkedList<>();

    public RouteFinder(@NotNull Level level, @NotNull Vector3 start, @NotNull Vector3 end) {
        this(level, start, end, true);
    }

    public RouteFinder(@NotNull Level level, @NotNull Vector3 start, @NotNull Vector3 end, boolean async) {
        this.startTick = Server.getInstance().getTick();
        this.level = level;
        this.start = start.floor();
        this.end = end.floor();

        this.distance = (int) start.distance(end);

        if (async) {
            Server.getInstance().getScheduler().scheduleAsyncTask(RsNPC.getInstance(), new AsyncTask() {
                @Override
                public void onRun() {
                    process();
                }
            });
        } else {
            this.process();
        }
    }

    /**
     * Pathfinding
     */
    private void process() {
        this.openNodes.add(new Node(this.start));

        Node nowNode;
        while ((nowNode = this.openNodes.poll()) != null && Server.getInstance().isRunning()) {
            //reached the end point, save the path
            if (nowNode.getVector3().equals(this.getEnd())) {
                Node parent = nowNode;
                parent.setVector3(parent.getVector3().add(0.5, 0, 0.5));
                this.nodes.add(parent);
                while ((parent = parent.getParent()) != null) {
                    parent.setVector3(parent.getVector3().add(0.5, 0, 0.5));
                    this.nodes.addFirst(parent);
                }
                break;
            }

            //break out on timeout (60s)
            if (Server.getInstance().getTick() - this.startTick > 20 * 60) {
                break;
            }

            LinkedList<Node> nextNodes = new LinkedList<>();

            for (int y = 1; y > -1; y--) {
                boolean N = this.check(nowNode, nextNodes, 0, y, -1);
                boolean E = this.check(nowNode, nextNodes, 1, y, 0);
                boolean S = this.check(nowNode, nextNodes, 0, y, 1);
                boolean W = this.check(nowNode, nextNodes, -1, y, 0);

                if (N && E) {
                    this.check(nowNode, nextNodes, 1, y, -1);
                }
                if (E && S) {
                    this.check(nowNode, nextNodes, 1, y, 1);
                }
                if (W && S) {
                    this.check(nowNode, nextNodes, -1, y, 1);
                }
                if (W && N) {
                    this.check(nowNode, nextNodes, -1, y, -1);
                }
            }

            if (nextNodes.isEmpty()) {
                continue;
            }

            this.openNodes.addAll(nextNodes);
            this.openNodes.sort((o1, o2) -> {
                double d1 = o1.getF();
                double d2 = o2.getF();
                if (d1 == d2) {
                    return 0;
                }
                return d1 > d2 ? 1 : -1;
            });
        }

        this.processingComplete = true;
    }

    private boolean check(Node nowNode, LinkedList<Node> nextNodes, int x, int y, int z) {
        Vector3 vector3 = nowNode.getVector3().add(x, y, z);
        if (this.closeNodes.contains(vector3)) {
            return false;
        }
        this.closeNodes.add(vector3);

        Node nextNode = new Node(vector3, nowNode, vector3.distance(this.start), vector3.distance(this.end));
        if (this.canMoveTo(nowNode, nextNode)) {
            nextNodes.add(nextNode);
            return true;
        }
        return false;
    }

    /**
     * Check whether movement to the target node is possible
     *
     * @param nowNode the current node
     * @param target  the target node
     * @return whether movement to the target node is possible
     */
    private boolean canMoveTo(Node nowNode, Node target) {
        if (!this.getBlockFast(target).canPassThrough() ||
                !this.getBlockFast(target.getVector3().add(0, 1, 0)).canPassThrough() ||
                !this.canWalkOn(this.getBlockFast(target.getVector3().add(0, -1, 0)))) {
            return false;
        }

        //jump check
        if (target.getVector3().getY() > nowNode.getVector3().getY() &&
                !this.getBlockFast(nowNode.getVector3().add(0, 2, 0)).canPassThrough()) {
            return false;
        }

        if (target.getVector3().getY() < nowNode.getVector3().getY() &&
                !this.getBlockFast(target.getVector3().add(0, 2, 0)).canPassThrough()) {
            return false;
        }

        return true;
    }

    private boolean canWalkOn(Block block) {
        if (block.getId() == Block.FLOWING_LAVA || block.getId() == Block.LAVA || block.getId() == Block.CACTUS) {
            return false;
        }
        if (block instanceof BlockFence || block instanceof BlockFenceGate) {
            return false;
        }
        if (block.getId() == Block.WATER || block.getId() == Block.FLOWING_WATER) {
            return true;
        }
        return !block.canPassThrough();
    }

    /**
     * Show the path with particles
     */
    public void show() {
        for (Node node : this.nodes) {
            this.level.addParticleEffect(node.getVector3(), ParticleEffect.REDSTONE_TORCH_DUST);
            this.level.addParticleEffect(node.getVector3().add(0.1, 0, 0.1), ParticleEffect.REDSTONE_TORCH_DUST);
            this.level.addParticleEffect(node.getVector3().add(0.1, 0, -0.1), ParticleEffect.REDSTONE_TORCH_DUST);
            this.level.addParticleEffect(node.getVector3().add(-0.1, 0, 0.1), ParticleEffect.REDSTONE_TORCH_DUST);
            this.level.addParticleEffect(node.getVector3().add(-0.1, 0, -0.1), ParticleEffect.REDSTONE_TORCH_DUST);
        }
    }

    /**
     * Quickly get a block
     *
     * @param node the node
     * @return the block
     */
    public Block getBlockFast(Node node) {
        return this.getBlockFast(node.getVector3());
    }

    /**
     * Quickly get a block
     *
     * @param vector3 the position
     * @return the block
     */
    public Block getBlockFast(Vector3 vector3) {
        return this.getBlockFast(vector3.getFloorX(), vector3.getFloorY(), vector3.getFloorZ());
    }

    /**
     * Quickly get a block
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block
     */
    public Block getBlockFast(int x, int y, int z) {
        if (!"Nukkit".equals(Server.getInstance().getName())) {
            return this.level.getBlock(x, y, z);
        }
        BlockState fullState = BlockAir.STATE;
        if (y >= 0 && y < 256) {
            int cx = x >> 4;
            int cz = z >> 4;
            IChunk chunk = this.getLevel().getChunk(cx, cz);

            if (chunk != null) {
                fullState = chunk.getBlockState(x & 15, y, z & 15);
            }
        }
        return fullState.toBlock(new Position(x, y, z, this.getLevel()));
    }

}
