package johnsmith.configoverhauled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
	public static final String MOD_ID = "config_overhauled";
	public static final String MOD_NAME = "Config Overhauled";
	public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final String COMMAND_ROOT = MOD_ID;
    public static final String COMMAND_LOCALIZATION = "localization";
    public static final String COMMAND_DUMP = "dump";

    public static final String ARG_MOD_ID = "mod_id";
    public static final String ARG_ONLY_MISSING_ENTRIES = "onlyMissingEntries";
    public static final String ARG_FORMAT = "format";
    public static final String ARG_SCOPE = "scope";
}