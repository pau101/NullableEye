package com.pau101.nullableeye.config.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.pau101.nullableeye.NullableEye;
import com.pau101.nullableeye.asm.NullableEyeLoadingPlugin;
import com.pau101.nullableeye.config.InspectorConfig;
import com.pau101.nullableeye.config.NullableEyeConfig;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Config(modid = NullableEye.ID)
@Config.RequiresMcRestart
public final class NullableEyeConfigData {
	private NullableEyeConfigData() {}

	public static InspectionScopeData inspectionScope = new InspectionScopeData();

	public static final class InspectionScopeData {
		private static final String[] DEFAULT = { "net.minecraft." };

		private InspectionScopeData() {}

		public String[] consumers = DEFAULT.clone();

		public String[] suppliers = DEFAULT.clone();
	}

	public static NullableEyeConfig create() {
		if (Loader.instance().getLoaderState() == LoaderState.NOINIT) {
			loadConfigInACompletelyNormalAndNotHackyInAnyWayMannerThatSurelyShouldntCauseIssuesDownTheRoad();
		}
		return new NullableEyeConfig() {
			@Override
			public InspectorConfig getInspectorConfig() {
				return new InspectorConfig() {
					@Override
					public boolean isConsumerInScope(String className) {
						return isInScope(className, inspectionScope.consumers);
					}

					@Override
					public boolean isConsumerRootInScope(String className) {
						for (String name : inspectionScope.consumers) {
							if (className.startsWith(name.substring(0, Math.min(className.length(), name.length())))) {
								return true;
							}
						}
						return false;
					}

					@Override
					public boolean isSupplierInScope(String className) {
						return isInScope(className, inspectionScope.suppliers);
					}

					private boolean isInScope(String target, String[] names) {
						for (String name : names) {
							if (target.startsWith(name)) {
								return true;
							}
						}
						return false;
					}
				};
			}
		};
	}

	private static void loadConfigInACompletelyNormalAndNotHackyInAnyWayMannerThatSurelyShouldntCauseIssuesDownTheRoad() {
		Map<String, Multimap<Config.Type, ASMDataTable.ASMData>> asm_data = ReflectionHelper.getPrivateValue(ConfigManager.class, null, "asm_data");
		Multimap<Config.Type, ASMDataTable.ASMData> map = HashMultimap.create();
		map.put(Config.Type.INSTANCE, new ASMDataTable.ASMData(null, null, NullableEyeConfigData.class.getName(), null, Collections.emptyMap()));
		asm_data.put(NullableEye.ID, map);
		File configDir;
		try {
			configDir = new File(NullableEyeLoadingPlugin.getMinecraftDir(), "config").getCanonicalFile();
		} catch (IOException e) {
			throw new LoaderException(e);
		}
		ReflectionHelper.setPrivateValue(Loader.class, Loader.instance(), configDir, "canonicalConfigDir");
		ConfigManager.sync(NullableEye.ID, Config.Type.INSTANCE);
	}
}
