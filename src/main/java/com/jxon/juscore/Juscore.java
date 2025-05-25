package com.jxon.juscore;

import com.jxon.juscore.commands.MarkovJuniorCommand;
import com.jxon.juscore.config.MarkovJuniorConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Juscore implements ModInitializer {
	public static final String MOD_ID = "juscore";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


	private static MarkovJuniorConfig config;

	@Override
	public void onInitialize() {

		LOGGER.info("MarkovJunior Mod initializing...");

		// 加载配置文件
		config = MarkovJuniorConfig.load();

		// 注册指令
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			MarkovJuniorCommand.register(dispatcher);
		});

		// 服务器启动事件
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("MarkovJunior Mod loaded successfully!");
			LOGGER.info("Use /mj help for command usage");
		});

		// 服务器关闭事件
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LOGGER.info("MarkovJunior Mod shutting down...");
		});

		LOGGER.info("MarkovJunior Mod initialized!");
	}

	/**
	 * 获取配置实例
	 */
	public static MarkovJuniorConfig getConfig() {
		return config;
	}

	/**
	 * 重新加载配置
	 */
	public static void reloadConfig() {
		config = MarkovJuniorConfig.load();
		LOGGER.info("MarkovJunior configuration reloaded");
	}
}