package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntitySelf;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "NoSlowB", setback = 5)
public class NoSlowB extends Check implements PacketCheck {

    private boolean lastSprinting = false;

    public NoSlowB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            boolean lastSprinting = this.lastSprinting;
            this.lastSprinting = player.isSprinting;

            if (!player.isSprinting) {
                return;
            }

            PacketEntitySelf selfEntity = player.compensatedEntities.getSelf();
            boolean passenger = selfEntity.inVehicle();

            // Players can sprint if they're able to fly (MCP)
            boolean canSprint = (passenger || player.food > 6.0F || player.canFly)
                    // Players can't start sprinting when gliding
                    && (lastSprinting || !player.isGliding)
                    // Players can't sprint while having blindness
                    && player.compensatedEntities.getPotionLevelForPlayer(PotionTypes.BLINDNESS) == null
                    // Players can sprint while being passengers of camels and other players
                    && (!passenger || canVehicleSprint(selfEntity.getRiding().type));

            if (!canSprint) {
                if (flag()) {
                    // Cancel the packet
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                    alert("");
                    player.getSetbackTeleportUtil().executeNonSimulatingSetback();

                    // Don't change last sprinting state, continue to set back player
                    this.lastSprinting = lastSprinting;
                }
            } else {
                reward();
            }
        }
    }

    private boolean canVehicleSprint(EntityType entityType) {
        return entityType == EntityTypes.CAMEL || entityType == EntityTypes.PLAYER;
    }
}
