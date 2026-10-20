package test;

import Pokemon.ElectricPokemon;
import Pokemon.FirePokemon;
import Pokemon.GrassPokemon;
import Pokemon.WaterPokemon;

public class TestRainDance {
    public static void main(String[] args) {
        WaterPokemon blastoise = new WaterPokemon("Blastoise", 40, 110, "Pokeflakes", "Blaaa");
        GrassPokemon venusaur = new GrassPokemon("Venusaur", 50, 100, "Pokeleafs", "Veee");
        ElectricPokemon raichu = new ElectricPokemon("Raichu", 80, 100, "Pokebrocks", "Raaai");
        FirePokemon charizard = new FirePokemon("Charrizard", 76, 100, "firenougats", "GRRR");
        WaterPokemon gyarados = new WaterPokemon("Gyarados", 90, 100, "Pokeflakes", "Gyaaa");
        WaterPokemon gyarados2 = new WaterPokemon("Gyarados", 90, 100, "Pokeflakes", "Gyaaa");
        WaterPokemon gyaradosKilled1 = new WaterPokemon("GyaradosKilled", 90, 5, "Pokeflakes", "Gyaaa");
        WaterPokemon gyaradosKilled2 = new WaterPokemon("GyaradosKilled", 90, 1, "Pokeflakes", "Gyaaa");
        GrassPokemon venusaur2 = new GrassPokemon("venusaur2", 90, 100, "Pokeflakes", "Gyaaa");

        System.out.println("=== Scenario 1: rainDance tegen GRASS (begin hp 100) ===");
        blastoise.rainDance(blastoise, venusaur);
        System.out.println("Verwacht hp na afloop: 110   Werkelijk: " + venusaur.getHp());

        System.out.println();
        System.out.println("=== Scenario 2: rainDance tegen ELECTRIC (begin hp 100) ===");
        blastoise.rainDance(blastoise, raichu);
        System.out.println("Verwacht hp na afloop: 100   Werkelijk: " + raichu.getHp());

        System.out.println();
        System.out.println("=== Scenario 3: rainDance tegen FIRE (begin hp 100) ===");
        blastoise.rainDance(blastoise, charizard);
        System.out.println("Verwacht hp na afloop: 85   Werkelijk: " + charizard.getHp());

        System.out.println();
        System.out.println("=== Scenario 4: rainDance tegen WATER (begin hp 100) ===");
        blastoise.rainDance(blastoise, gyarados);
        System.out.println("Verwacht hp na afloop: 95   Werkelijk: " + gyarados.getHp());

        System.out.println();
        System.out.println("=== Scenario 5: rainDance tegen WATER (begin hp 5) ===");
        blastoise.rainDance(blastoise, gyaradosKilled1);
        System.out.println("Verwacht hp na afloop: 0   Werkelijk: " + gyaradosKilled1.getHp());

        System.out.println();
        System.out.println("=== Scenario 6: rainDance tegen WATER (begin hp 1) ===");
        blastoise.rainDance(blastoise, gyaradosKilled2);
        System.out.println("Verwacht hp na afloop: 0   Werkelijk: " + gyaradosKilled2.getHp());

        System.out.println();
        System.out.println("=== Scenario 7: leechSeed tegen ANY (Water als test) (begin hp 100) ===");
        GrassPokemon kopie = new GrassPokemon(venusaur2.getName(), venusaur2.getLevel(), venusaur2.getHp(), venusaur2.getFood(), venusaur2.getSound());
        kopie.leechSeed(venusaur2, gyarados2);
        System.out.println("Verwacht enemy hp na afloop: 90   Werkelijk: " + gyarados2.getHp());
        System.out.println("Verwacht attacker hp na afloop: 110   Werkelijk: " + venusaur2.getHp());
    }
}
