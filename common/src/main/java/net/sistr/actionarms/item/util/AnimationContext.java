package net.sistr.actionarms.item.util;

import java.util.UUID;
import net.minecraft.world.World;
import net.sistr.actionarms.network.ItemAnimationEventPacket;

public interface AnimationContext {

  static AnimationContext of(World world, UUID uuid) {
    return (animation, seconds) ->
        ItemAnimationEventPacket.sendS2C(world, uuid, animation, seconds);
  }

  void setAnimation(String animation, float seconds);
}
