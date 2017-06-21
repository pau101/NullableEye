package com.pau101.nullableeye.asm;

import com.pau101.nullableeye.NullableEye;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.asm.ASMTransformerWrapper;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

@IFMLLoadingPlugin.Name(NullableEye.NAME)
@IFMLLoadingPlugin.MCVersion(MinecraftForge.MC_VERSION)
@IFMLLoadingPlugin.TransformerExclusions("com.pau101.nullableeye.")
@IFMLLoadingPlugin.SortingIndex(1000000)
public final class NullableEyeLoadingPlugin implements IFMLLoadingPlugin {
	private static File minecraftDir;

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {
			"com.pau101.nullableeye.asm.RuntimeClassBytesProvider$ClassBytesInterceptor",
			"com.pau101.nullableeye.asm.RuntimeNullityProvider$NullityInterceptor"
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
		ClassLoader cl = NullableEyeLoadingPlugin.class.getClassLoader();
		if (cl instanceof LaunchClassLoader) {
			LaunchClassLoader lcl = (LaunchClassLoader) cl;
			List<IClassTransformer> transformers = ReflectionHelper.getPrivateValue(LaunchClassLoader.class, lcl, "transformers");
			catchPatchingTransformer(transformers);
		}
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

	private void catchPatchingTransformer(List<IClassTransformer> transformers) {
		ListIterator<IClassTransformer> iter = transformers.listIterator();
		while (iter.hasNext()) {
			IClassTransformer item = iter.next();
			IClassTransformer transformer;
			if (item instanceof ASMTransformerWrapper.TransformerWrapper) {
				transformer = ReflectionHelper.getPrivateValue(ASMTransformerWrapper.TransformerWrapper.class, (ASMTransformerWrapper.TransformerWrapper) item, "parent");
			} else {
				transformer = item;
			}
			String name = transformer.getClass().getName();
			int dot = name.lastIndexOf('.');
			iter.set(new MemberPatchRelocator.Catcher(dot == -1 ? "" : name.substring(0, dot + 1), item));
		}
	}

	public static File getMinecraftDir() {
		return minecraftDir;
	}
}
