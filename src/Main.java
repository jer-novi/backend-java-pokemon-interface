import Pokemon.ElectricPokemon;
import Pokemon.FirePokemon;
import Pokemon.GrassPokemon;
import Pokemon.Pokemon;
import Pokemon.WaterPokemon;
import audio.SoundEngine;
import gym.PokemonGymImpl;
import gym.PokemonTrainer;
import io.GameQuitException;
import io.JLineIO;
import ui.ArcadeUI;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    // Deze 4 constanten worden gebruikt om tekst met kleur te printen in de console.
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    // Dit zijn de Pokemons die je tot je beschikking hebt
    private static final FirePokemon charizard = new FirePokemon("Charrizard", 76, 150, "firenougats", "GRRRRRRRRRRRRRRR");
    private static final WaterPokemon blastoise = new WaterPokemon("Blastoise", 40, 110, "Pokeflakes", "Blaaaaasssssstooooiiiiissss");
    private static final GrassPokemon venusaur = new GrassPokemon("Venusaur", 50, 135, "Pokeleafs", "Veeeeeeeeennnnnuuuuuusaur");
    private static final GrassPokemon ditto = new GrassPokemon("Ditto", 60, 140, "Everything", "Dittto diiiito ");
    private static final ElectricPokemon raichu = new ElectricPokemon("Raichu", 80, 160, "Pokebrocks", "Raaaaiiiiicccchhhhuuuuuuu!!!!");
    private static final WaterPokemon gyarados = new WaterPokemon("Gyarados", 90, 180, "Pokeflakes", "Gyaaaaaaaaarrrraaaadoooos");

    private static final List<Pokemon> pokemons = Arrays.asList(charizard, blastoise, venusaur, ditto, raichu, gyarados);

    public static void main(String[] args) throws IOException {
        try (JLineIO io = new JLineIO()) {
            ArcadeUI.clearScreen(io);
            ArcadeUI.printBanner(io);

            PokemonGymImpl pokemonGym = new PokemonGymImpl(pokemons, io);

            String userA = io.readLine(ArcadeUI.YELLOW + "First player please enter your name:" + ArcadeUI.RESET);
            if (userA.isBlank()) {
                userA = "Ash";
            }
            PokemonTrainer player1 = new PokemonTrainer(userA, pokemons);

            int savedBadges = ArcadeUI.getPlayerBadges(userA);
            if (savedBadges > 0) {
                ArcadeUI.soundBell();
                io.println(ArcadeUI.GREEN + "🌟 Welcome back, " + userA + "! Saved profile loaded: " + savedBadges + " Gym Badge(s) 🏅" + ArcadeUI.RESET);
            } else {
                io.println(ArcadeUI.CYAN + "👋 Welcome, new trainer " + userA + "!" + ArcadeUI.RESET);
            }

            // 1. ASCII Art stijl keuze (typ 1, 2, 'color' of 'normal')
            String artChoice = io.readLine("Choose ASCII art style — 1) color (Pixel/Divoom)  2) normal (Classic ASCII):", List.of("color", "normal"));
            while (!artChoice.equalsIgnoreCase("color") && !artChoice.equalsIgnoreCase("normal")) {
                io.println("Please enter 1 (color) or 2 (normal).");
                artChoice = io.readLine("Choose ASCII art style — 1) color  2) normal:", List.of("color", "normal"));
            }
            ArcadeUI.setUseColoredArt(artChoice.equalsIgnoreCase("color"));

            // 2. Sound Mode keuze (typ 1, 2, 3 of 'multi', 'single', 'sfx')
            String audioMode = io.readLine("Choose sound mode — 1) multi (Multi-Channel JSON)  2) single (1-Ch GameBoy)  3) sfx (SFX Only):",
                    List.of("multi", "single", "sfx"));
            while (!audioMode.equalsIgnoreCase("multi")
                    && !audioMode.equalsIgnoreCase("single")
                    && !audioMode.equalsIgnoreCase("sfx")) {
                io.println("Please enter 1 (multi), 2 (single) or 3 (sfx).");
                audioMode = io.readLine("Choose sound mode — 1) multi  2) single  3) sfx:", List.of("multi", "single", "sfx"));
            }

            if (audioMode.equalsIgnoreCase("multi")) {
                io.println(ArcadeUI.CYAN + "  Select Multi-Channel Soundtrack:" + ArcadeUI.RESET);
                io.println("    " + ArcadeUI.YELLOW + "1)" + ArcadeUI.RESET + " Poke-Red-OG (Trainer Battle)");
                io.println("    " + ArcadeUI.YELLOW + "2)" + ArcadeUI.RESET + " Poke-2nd-Choice (20-Channel Remix)");
                io.println("    " + ArcadeUI.YELLOW + "3)" + ArcadeUI.RESET + " Poke-3rd-Choice (6-Channel Chiptune)");
                String trackChoice = io.readLine("Choose track (1/2/3):", List.of("1", "2", "3"));
                while (!trackChoice.equals("1") && !trackChoice.equals("2") && !trackChoice.equals("3")) {
                    io.println("Please choose 1, 2 or 3.");
                    trackChoice = io.readLine("Choose track (1/2/3):", List.of("1", "2", "3"));
                }
                switch (trackChoice) {
                    case "2" -> SoundEngine.setMusicMode(SoundEngine.MusicMode.MULTI_2);
                    case "3" -> SoundEngine.setMusicMode(SoundEngine.MusicMode.MULTI_3);
                    default -> SoundEngine.setMusicMode(SoundEngine.MusicMode.MULTI_1);
                }
            } else if (audioMode.equalsIgnoreCase("single")) {
                SoundEngine.setMusicMode(SoundEngine.MusicMode.SINGLE);
            } else {
                SoundEngine.setMusicMode(SoundEngine.MusicMode.SFX_ONLY);
            }

            io.println("\nTo start your game, we have given you 6 Pokemons to use.");
            io.println("These are the Pokemons in your party:");
            pokemonGym.printPokemon(pokemons);
            io.println("");

            // 3. Gym binnenstappen (typ y, n, yes, no, 1 of 2)
            String answer = io.readLine("Would you like to enter a gym looking for a fight? (y/n):", List.of("yes", "no"));
            while (!answer.equalsIgnoreCase("yes") && !answer.equalsIgnoreCase("no")) {
                io.println("Invalid choice. Please enter 'y' (yes) or 'n' (no).");
                answer = io.readLine("Would you like to enter a gym looking for a fight? (y/n):", List.of("yes", "no"));
            }

            if (answer.equalsIgnoreCase("yes")) {
                pokemonGym.enteredTheGym(player1);
            } else {
                io.println("Maybe next time! Goodbye.");
            }
        } catch (GameQuitException e) {
            System.out.println("\nThank you for playing");
        }
    }
}
