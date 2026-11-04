package dev.isxander.debugify.config;

public enum Issue3Tweak {
    DROPPED_ITEMS_RING_BELLS("dropped_items_ring_bells"),
    TAME_SKELETON_HORSES_ON_INTERACTION("tame_skeleton_horses_on_interaction"),
    VISIBLE_VILLAGER_ARMOR("visible_villager_armor");

    private final String key;

    Issue3Tweak(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public String labelKey() {
        return "debugify.issue3_tweak." + key;
    }

    public String descriptionKey() {
        return labelKey() + ".description";
    }
}
