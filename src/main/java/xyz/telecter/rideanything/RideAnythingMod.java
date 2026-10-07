package xyz.telecter.rideanything;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import xyz.telecter.rideanything.config.RideAnythingConfig;

public class RideAnythingMod implements ModInitializer {
	public static final String MOD_ID = "rideanything";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Set<UUID> RIDE_ENABLED_PLAYERS = new HashSet<>();

	@Override
	public void onInitialize() {
		RideAnythingConfig.HANDLER.load();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> RIDE_ENABLED_PLAYERS.clear());

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("rideanything")
						.requires(source -> true)
						.then(Commands.literal("on").executes(context -> setRideEnabled(
								context.getSource().getPlayerOrException(), true)))
						.then(Commands.literal("off").executes(context -> setRideEnabled(
								context.getSource().getPlayerOrException(), false)))
						.then(Commands.literal("toggle").executes(RideAnythingMod::toggleRideEnabled))));

		UseEntityCallback.EVENT.register((player, world, hand, entity, result) -> {
			if (!world.isClientSide() && RIDE_ENABLED_PLAYERS.contains(player.getUUID())
					&& RideAnythingConfig.HANDLER.instance().enabled) {
				if (player.getMainHandItem().isEmpty() && shouldRide(player, entity)) {
					if (player.startRiding(entity)) {
						return InteractionResult.SUCCESS;
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	private static int toggleRideEnabled(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		return setRideEnabled(player, !RIDE_ENABLED_PLAYERS.contains(player.getUUID()));
	}

	private static int setRideEnabled(ServerPlayer player, boolean enabled) {
		boolean changed = enabled ? RIDE_ENABLED_PLAYERS.add(player.getUUID())
				: RIDE_ENABLED_PLAYERS.remove(player.getUUID());
		String message = enabled
				? (changed ? "右键骑乘已启用" : "右键骑乘已经处于启用状态！")
				: (changed ? "右键骑乘已禁用" : "右键骑乘已经处于禁用状态！");

		player.sendSystemMessage(Component.literal(message).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
		return 1;
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
