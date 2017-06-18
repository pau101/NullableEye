package com.pau101.nullableeye.asm;

import com.pau101.nullableeye.NullableEye;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.Name(NullableEye.NAME)
@IFMLLoadingPlugin.MCVersion(MinecraftForge.MC_VERSION)
@IFMLLoadingPlugin.TransformerExclusions("com.pau101.nullableeye.")
public final class NullableEyeLoadingPlugin implements IFMLLoadingPlugin {
	private static File minecraftDir;

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {
			RuntimeClassBytesProvider.ClassBytesIntercepter.class.getName(),
			RuntimeNullityProvider.NullityIntercepter.class.getName()
		};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		minecraftDir = (File) data.get("mcLocation");
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	public static File getMinecraftDir() {
		return minecraftDir;
	}
}
