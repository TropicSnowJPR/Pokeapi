package com.pokeapi.papi;

import com.google.gson.annotations.SerializedName;

import java.util.List;

record Cries(String latest, String legacy) {
}

record Sprites(
        @SerializedName("front_default") String frontDefault,
        OtherSprites other) {}

record OtherSprites(@SerializedName("official-artwork") OfficialArtwork officialArtwork) {}
record OfficialArtwork(@SerializedName("front_default") String frontDefault) {}
record InnerType(String name) {}
record Type(@SerializedName("type") InnerType inner) {}
record InnerAbility(String name) {}
record Ability(@SerializedName("ability") InnerAbility inner) {}
class InnerStats {
    private String name;

    public InnerStats(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
record Stats(@SerializedName("stat") InnerStats inner, @SerializedName("base_stat") int baseStat) {}
record Form(String name) {}
record InnerMove(String name) {}
record Move(@SerializedName("move") InnerMove inner) {}

public record Pokemon(
        int id,
        String name,
        String generation,
        int height,
        int weight,
        Cries cries,
        List<Type> types,
        List<Ability> abilities,
        List<Stats> stats,
        List<Form> forms,
        List<Move> moves,
        Sprites sprites
) {
}
