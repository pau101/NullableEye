package com.pau101.nullableeye;

import com.pau101.nullableeye.annotation.IntelliJNullityAnnotationWriter;
import com.pau101.nullableeye.asm.RuntimeClassBytesProvider;
import com.pau101.nullableeye.asm.RuntimeNullityProvider;
import com.pau101.nullableeye.config.NullableEyeConfig;
import com.pau101.nullableeye.config.data.NullableEyeConfigData;
import com.pau101.nullableeye.inspector.Inspector;
import com.pau101.nullableeye.mappings.Mappings;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Paths;

@Mod(
	modid = NullableEye.ID,
	name = NullableEye.NAME,
	version = NullableEye.VERSION,
	acceptableRemoteVersions = "*"
)
public final class NullableEye {
	public static final String ID = "nullableeye";

	public static final String NAME = "Nullable Eye";

	public static final String VERSION = "1.0.0";

	private static final Logger LOGGER = LogManager.getLogger(NAME);

	private static NullableEye instance;

	private final NullableEyeConfig config = NullableEyeConfigData.create();

	private final Inspector inspector = new Inspector(LOGGER, config.getInspectorConfig(), RuntimeClassBytesProvider.instance(), RuntimeNullityProvider.instance());

	private NullableEye() {}

	@Mod.EventHandler
	public void init(FMLPostInitializationEvent event) {
		ClassLoader loader = getClass().getClassLoader();
		if (loader instanceof URLClassLoader) {
			Remapper remapper;
			try {
				remapper = Mappings.load(String.format("/assets/%s/srg-mcp.srg", ID));
			} catch (IOException e) {
				LOGGER.info("Unable to load mcp mappings, will output in srg", e);
				remapper = new Remapper() {};
			}
			inspector.inspect(((URLClassLoader) loader).getURLs());
			new IntelliJNullityAnnotationWriter(LOGGER).write(inspector.getInspections(), remapper, Paths.get("annotations"));
		} else {
			LOGGER.warn("Unable to inspect because of unknown class loader \"{}\"", loader.getClass().getName());
		}
	}

	public Inspector getInspector() {
		return inspector;
	}

	@Mod.InstanceFactory
	public static NullableEye instance() {
		if (instance == null) {
			instance = new NullableEye();
		}
		return instance;
	}

	public static Logger getLogger() {
		return LOGGER;
	}
}
