package dev.isxander.debugify.config;

/**
 * Optional behaviour tweaks requested in GitHub issue #2. These are NOT bug
 * fixes: they deliberately alter vanilla behaviour, so they are all disabled
 * by default, applied server-side only, and never negotiated with clients.
 */
public enum Issue2Tweak {
    WATER_BREATHING_PROTECTS_FISH("water_breathing_protects_fish"),
    JACK_O_LANTERN_SNOW_GOLEMS("jack_o_lantern_snow_golems"),
    UNDERWATER_CAULDRONS("underwater_cauldrons"),
    NETHER_CAULDRON_EVAPORATION("nether_cauldron_evaporation"),
    CUSHIONED_ANVILS("cushioned_anvils"),
    BURNING_SNOWBALLS("burning_snowballs"),
    ARROWS_TRIGGER_FALLING_BLOCKS("arrows_trigger_falling_blocks"),
    RESTRICT_SEAGRASS_SUBSTRATES("restrict_seagrass_substrates"),
    PREVENT_UNSUPPORTED_PLANT_GROWTH("prevent_unsupported_plant_growth"),
    END_PORTALS_REQUIRE_FRAMES("end_portals_require_frames"),
    LAVA_CONVERTS_WATERLOGGED_CORAL("lava_converts_waterlogged_coral"),
    VILLAGER_BEDS_EXPLODE_IN_INVALID_DIMENSIONS("villager_beds_explode_in_invalid_dimensions");

    private final String key;

    Issue2Tweak(String key) {
        this.key = key;
    }

    /**
     * JSON key inside the {@code issue_2_tweaks} object of debugify.json.
     */
    public String key() {
        return key;
    }

    public String labelKey() {
        return "debugify.issue2_tweak." + key;
    }

    public String descriptionKey() {
        return labelKey() + ".description";
    }
}
