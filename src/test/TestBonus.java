package test;

import Pokemon.*;
import gym.PokemonGymImpl;
import gym.PokemonGymOwner;
import gym.PokemonTrainer;

import java.util.*;

public class TestBonus {
    public static void main(String[] args) {
        FirePokemon charizard = new FirePokemon("Charrizard", 76, 150, "firenougats", "GRR");
        GrassPokemon ditto = new GrassPokemon("Ditto", 60, 140, "Everything", "D");
        WaterPokemon blastoise = new WaterPokemon("Blastoise", 40, 110, "Pokeflakes", "B");
        List<Pokemon> list = Arrays.asList(charizard, ditto, blastoise);

        charizard.eat();
        System.out.println("eat: verwacht 170, werkelijk " + charizard.getHp());
        System.out.println("likes: " + charizard.likes("FireNougats") + " (true) " + charizard.likes("pizza") + " (false) " + ditto.likes("pizza") + " (true)");

        PokemonGymImpl gym = new PokemonGymImpl(list);
        PokemonTrainer trainer = new PokemonTrainer("Jerome", list);
        blastoise.setHp(0);
        System.out.println("fainted: " + blastoise.isFainted() + " (true)");
        System.out.println("select dode: verwacht Charrizard, werkelijk " + gym.selectPokemon("blastoise", trainer).getName());
        System.out.println("select Ditto: " + gym.selectPokemon("ditto", trainer).getName());
        System.out.println("select typo: " + gym.selectPokemon("xxx", trainer).getName() + " (Charrizard)");

        Pokemon kopie = charizard.copy();
        kopie.setHp(1);
        System.out.println("copy onafhankelijk: origineel " + charizard.getHp() + " (170) kopie " + kopie.getHp() + " (1)");
        Pokemon gymKopie = gym.chooseGymPokemon(new PokemonGymOwner("B", "P", list));
        System.out.println("gym-keuze leeft: " + !gymKopie.isFainted());
    }
}
