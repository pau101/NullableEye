package com.pau101.nullableeye;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name(NullableEyeLoadingPlugin.NAME)
@IFMLLoadingPlugin.MCVersion(MinecraftForge.MC_VERSION)
@IFMLLoadingPlugin.TransformerExclusions("com.pau101.nullableeye.")
public final class NullableEyeLoadingPlugin implements IFMLLoadingPlugin {
	public static final String NAME = "Nullable Eye";

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { NullableEyeClassTransformer.class.getName() };
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
	public void injectData(Map<String, Object> data) {}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
