// 修改 src/main/java/com/jxon/juscore/Juscore.java
package com.jxon.juscore;

import com.jxon.juscore.config.MarkovWorldConfig;
import com.jxon.juscore.worldgen.MarkovWorldType;
import com.jxon.juscore.resources.ResourceManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Juscore implements ModInitializer {
	public static final String MOD_ID = "juscore";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static MarkovWorldConfig globalConfig = new MarkovWorldConfig();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing MarkovJunior World Generator");

		// 注册世界类型
		MarkovWorldType.register();

		// 初始化资源管理器
		ResourceManager.initialize();

		LOGGER.info("MarkovJunior World Generator initialized successfully");
	}

	// 添加缺失的配置方法
	public static MarkovWorldConfig getConfig() {
		return globalConfig;
	}

	public static void setConfig(MarkovWorldConfig config) {
		globalConfig = config;
	}

	public static void reloadConfig() {
		// 重新加载配置（暂时只是重置为默认配置）
		globalConfig = new MarkovWorldConfig();
		LOGGER.info("Configuration reloaded");
	}
}