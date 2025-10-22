package com.pokeapi.papi;

import com.google.gson.annotations.SerializedName;

public record Moves(
        int id,
        String name,
        Integer power,
        Integer accuracy,
        @SerializedName("type") Type moveType,
        @SerializedName("damage_class") DamageClass damageClass,
        @SerializedName("effect_chance") Integer effectChance
) {
    public record Type(String name) {}
    public record DamageClass(String name) {}
}
