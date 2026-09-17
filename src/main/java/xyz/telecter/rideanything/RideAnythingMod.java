package xyz.telecter.rideanything;

import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import xyz.telecter.rideanything.config.RideAnythingConfig;

public class RideAnythingMod implements ModInitializer {
	public static final String MOD_ID = "rideanything";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		RideAnythingConfig.HANDLER.load();

		UseEntityCallback.EVENT.register((player, world, hand, entity, result) -> {
			if (!world.isClientSide() && RideAnythingConfig.HANDLER.instance().enabled) {
				if (player.getMainHandItem().isEmpty() && shouldRide(player, entity)) {
					if (player.startRiding(entity)) {
						return InteractionResult.SUCCESS;
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	public static boolean shouldRide(Player player, Entity entity) {
		RideAnythingConfig config = RideAnythingConfig.HANDLER.instance();
		return switch (config.mode) {
			case ANIMALS -> entity instanceof Animal;
			case ALL -> entity instanceof Mob;
			case CUSTOM -> isListed(entity, config.allowed);
			case BLACKLIST -> entity instanceof Mob && !isListed(entity, config.denied);
		};
	}

	private static boolean isListed(Entity entity, Iterable<String> configuredEntities) {
		Identifier entityId = EntityType.getKey(entity.getType());

		for (String configuredEntity : configuredEntities) {
			if (entityId.equals(Identifier.parse(configuredEntity))) {
				return true;
			}
		}

		return false;
	}
}
