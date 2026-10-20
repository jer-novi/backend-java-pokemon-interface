package test;

import Pokemon.FirePokemon;
import Pokemon.GrassPokemon;

public class TestFireLash {
    public static void main(String[] args) {
        FirePokemon charizard = new FirePokemon("Charrizard", 76, 150, "firenougats", "GRRR");
        GrassPokemon venusaur = new GrassPokemon("Venusaur", 50, 135, "Pokeleafs", "Veeeee");

        System.out.println("Voor de aanval:  Venusaur hp = " + venusaur.getHp());
        charizard.fireLash(charizard, venusaur);
        System.out.println("Na de aanval:    Venusaur hp = " + venusaur.getHp());
        System.out.println("Charizard hp     = " + charizard.getHp());
    }
}
