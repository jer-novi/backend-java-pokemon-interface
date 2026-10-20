package gym;

import Pokemon.ElectricPokemon;
import Pokemon.FirePokemon;
import Pokemon.GrassPokemon;
import Pokemon.Pokemon;
import Pokemon.WaterPokemon;
import audio.SoundEngine;
import io.ChoiceHelper;
import io.GameIO;
import ui.ArcadeUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;

// Los in deze klasse alle foutmeldingen op door (abstracte) klassen met variabelen en methodes te maken en een interface met methodes (en soms een import).
public class PokemonGymImpl implements PokemonGym {

    List<Pokemon> pokemons;
    // Brock krijgt eigen kopieen, zodat hp van de speler en van Brock los van elkaar staan.
    private final List<Pokemon> gymPokemons;
    private final GameIO io;
    private Pokemon activePlayerPokemon;

    public PokemonGymImpl(List<Pokemon> pokemons, GameIO io) {
        this.pokemons = pokemons;
        this.gymPokemons = pokemons.stream().map(Pokemon::copy).toList();
        this.io = io;
    }

    public PokemonGymImpl(List<Pokemon> pokemons) {
        this(pokemons, new GameIO() {
            @Override
            public void println(String text) {
                System.out.println(text);
            }

            @Override
            public String readLine(String prompt) {
                System.out.print(prompt + " ");
                return new Scanner(System.in).nextLine();
            }
        });
    }

    @Override
    public void enteredTheGym(PokemonTrainer player1) {
        PokemonGymOwner gymOwner = new PokemonGymOwner("Brock", "Pewter City", gymPokemons);
        if (allFainted(player1.getPokemons())) {
            io.println(ArcadeUI.RED + "💀 All your pokemons have fainted. Restart the game to play again." + ArcadeUI.RESET);
            return;
        }
        if (allFainted(gymOwner.getPokemons())) {
            SoundEngine.playVictoryFanfare();
            int badges = ArcadeUI.incrementPlayerBadges(player1.getName());
            io.println(ArcadeUI.YELLOW + ArcadeUI.BOLD + "🏆 " + gymOwner.getName() + " has no pokemons left to fight. You have won the Pewter Badge!" + ArcadeUI.RESET);
            io.println(ArcadeUI.GREEN + "💾 Game Saved! Total Badges for " + player1.getName() + ": " + badges + " 🏅" + ArcadeUI.RESET);
            return;
        }

        ArcadeUI.clearScreen(io);
        SoundEngine.startBattleMusic();
        int currentBadges = ArcadeUI.getPlayerBadges(player1.getName());
        io.println(ArcadeUI.CYAN + "📍 You have entered the " + gymOwner.getTown() + " gym (Your Badges: " + currentBadges + " 🏅)  " + SoundEngine.getStatusText() + ArcadeUI.RESET);
        io.println("In front of you stands a pokemontrainer...");
        ArcadeUI.typewriter(io, ArcadeUI.RED + gymOwner.getName() + ArcadeUI.RESET + ": Hello stranger, I'm " + gymOwner.getName() + ", the owner of this gym. Who are you?", 12);
        ArcadeUI.typewriter(io, ArcadeUI.GREEN + player1.getName() + ArcadeUI.RESET + ": I'm " + player1.getName() + " and I'm here to challenge you for a battle!", 12);
        ArcadeUI.typewriter(io, ArcadeUI.RED + gymOwner.getName() + ArcadeUI.RESET + ": So you're after my badge too, let's fight!!!", 12);

        Pokemon gymPokemon = chooseGymPokemon(gymOwner);
        ArcadeUI.playPokeballAnimation(io, ArcadeUI.RED, gymOwner.getName(), gymPokemon);

        Pokemon pokemon = choosePokemon(player1);
        ArcadeUI.playPokeballAnimation(io, ArcadeUI.GREEN, player1.getName(), pokemon);

        ArcadeUI.syncDivoomState("battle_start", pokemon, gymPokemon, "none");
        fightRound(player1, gymOwner, pokemon, gymPokemon);
    }

    private boolean allFainted(List<Pokemon> list) {
        return list.stream().allMatch(Pokemon::isFainted);
    }

    @Override
    public void printPokemon(List<Pokemon> pokemons) {
        for (int i = 0; i < pokemons.size(); i++) {
            io.println("  " + ChoiceHelper.getBadgeForIndex(i) + " " + ArcadeUI.renderHpBar(pokemons.get(i)));
        }
    }

    @Override
    public Pokemon selectPokemon(String pokemon, PokemonTrainer trainer) {
        // Alleen Pokemons met hp > 0 zijn kiesbaar; bij een typefout pakken we de eerste die nog kan vechten.
        Pokemon firstAlive = null;
        for (Pokemon p : trainer.getPokemons()) {
            if (p.isFainted()) {
                continue;
            }
            if (p.getName().equalsIgnoreCase(pokemon)) {
                return p;
            }
            if (firstAlive == null) {
                firstAlive = p;
            }
        }
        return firstAlive;
    }

    @Override
    public void fightRound(PokemonTrainer trainer, PokemonGymOwner owner, Pokemon pokemon, Pokemon gymPokemon) {
        this.activePlayerPokemon = pokemon;
        Pokemon activeGymPokemon = gymPokemon;

        while (!allFainted(trainer.getPokemons()) && !allFainted(owner.getPokemons())) {
            ArcadeUI.printSectionSpacing(io);

            // 1. Beurt van Gym Leader Brock
            io.println(ArcadeUI.RED + "▶ It's " + owner.getName() + "'s turn to attack!" + ArcadeUI.RESET);
            SoundEngine.playAttackSfx(activeGymPokemon.getPokemonType());
            gymOwnerAttacks(activeGymPokemon, activePlayerPokemon);
            ArcadeUI.syncDivoomState("gym_attack", activePlayerPokemon, activeGymPokemon, "enemy_attack");

            if (activePlayerPokemon.isFainted()) {
                SoundEngine.playFaintSfx();
                ArcadeUI.syncDivoomState("player_fainted", activePlayerPokemon, activeGymPokemon, "defeat");
                io.println(ArcadeUI.RED + "💀 " + activeGymPokemon.getName() + " has defeated your " + activePlayerPokemon.getName() + "!" + ArcadeUI.RESET);
                if (allFainted(trainer.getPokemons())) {
                    break;
                }
                long aliveCount = trainer.getPokemons().stream().filter(p -> !p.isFainted()).count();
                io.println(ArcadeUI.YELLOW + "⚡ You still have " + aliveCount + " Pokémon left! Choose your next Pokémon:" + ArcadeUI.RESET);
                activePlayerPokemon = choosePokemon(trainer);
                ArcadeUI.playPokeballAnimation(io, ArcadeUI.GREEN, trainer.getName(), activePlayerPokemon);
            } else if ((double) activePlayerPokemon.getHp() / activePlayerPokemon.getMaxHp() <= 0.25) {
                SoundEngine.playLowHpWarning();
            }

            // 2. Beurt van de Speler (HUD wordt 1x per ronde getekend vlak voor jouw keuze!)
            ArcadeUI.printSectionSpacing(io);
            ArcadeUI.printBattleHud(io, trainer.getName(), activePlayerPokemon, trainer.getPokemons(),
                    owner.getName(), activeGymPokemon, owner.getPokemons());
            io.println(ArcadeUI.GREEN + "▶ It's " + trainer.getName() + "'s turn to attack!" + ArcadeUI.RESET);
            attackOrChange(activePlayerPokemon, activeGymPokemon, trainer, owner);
            ArcadeUI.syncDivoomState("player_turn_done", activePlayerPokemon, activeGymPokemon, "player_action");

            if (activeGymPokemon.isFainted()) {
                SoundEngine.playFaintSfx();
                io.println(ArcadeUI.GREEN + "🎉 Your " + activePlayerPokemon.getName() + " has defeated " + owner.getName() + "'s " + activeGymPokemon.getName() + "!" + ArcadeUI.RESET);
                if (allFainted(owner.getPokemons())) {
                    break;
                }
                long gymAliveCount = owner.getPokemons().stream().filter(p -> !p.isFainted()).count();
                io.println(ArcadeUI.RED + "⚔️ " + owner.getName() + " has " + gymAliveCount + " Pokémon left and reaches for another Pokéball!" + ArcadeUI.RESET);
                activeGymPokemon = chooseGymPokemon(owner);
                ArcadeUI.playPokeballAnimation(io, ArcadeUI.RED, owner.getName(), activeGymPokemon);
            } else if ((double) activeGymPokemon.getHp() / activeGymPokemon.getMaxHp() <= 0.25) {
                SoundEngine.playLowHpWarning();
            }
        }

        if (allFainted(owner.getPokemons())) {
            SoundEngine.playVictoryFanfare();
            int badges = ArcadeUI.incrementPlayerBadges(trainer.getName());
            ArcadeUI.syncDivoomState("gym_fainted_victory", activePlayerPokemon, activeGymPokemon, "victory");
            io.println(ArcadeUI.YELLOW + ArcadeUI.BOLD + "\n🏆 " + owner.getName() + " has no Pokémon left to fight! You have won the Pewter Badge!" + ArcadeUI.RESET);
            io.println(ArcadeUI.GREEN + "💾 Game Saved! Total Badges for " + trainer.getName() + ": " + badges + " 🏅" + ArcadeUI.RESET);
        } else {
            SoundEngine.stopBattleMusic();
            io.println(ArcadeUI.RED + "\n💀 All 6 of your Pokémon have fainted! " + owner.getName() + " wins the Gym Battle." + ArcadeUI.RESET);
        }

        String keepPlaying = io.readLine("Would you like to heal at the Pokémon Center and challenge Brock again? (y/n):", List.of("yes", "no"));
        while (!keepPlaying.equalsIgnoreCase("yes") && !keepPlaying.equalsIgnoreCase("no")) {
            io.println("Invalid choice. Please enter 'y' (yes) or 'n' (no).");
            keepPlaying = io.readLine("Would you like to heal at the Pokémon Center and challenge Brock again? (y/n):", List.of("yes", "no"));
        }
        if (keepPlaying.equalsIgnoreCase("yes")) {
            for (Pokemon p : trainer.getPokemons()) p.setHp(p.getMaxHp());
            for (Pokemon p : gymPokemons) p.setHp(p.getMaxHp());
            SoundEngine.playHealJingle();
            enteredTheGym(trainer);
        } else {
            SoundEngine.stopBattleMusic();
            io.println(ArcadeUI.CYAN + "Thank you for playing!" + ArcadeUI.RESET);
        }
    }

    @Override
    public Pokemon chooseGymPokemon(PokemonGymOwner gymOwner) {
        Random rand = new Random();
        List<Pokemon> pokemons = new ArrayList<>();
        for (Pokemon p : gymOwner.getPokemons()) {
            if (p.getHp() > 0) {
                pokemons.add(p);
            }
        }
        int amountOfPokemons = pokemons.size();
        int randomNumber = rand.nextInt(amountOfPokemons);
        return pokemons.get(randomNumber);
    }

    @Override
    public Pokemon choosePokemon(PokemonTrainer trainer) {
        List<Pokemon> pokemons = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Pokemon p : trainer.getPokemons()) {
            if (p.getHp() > 0) {
                pokemons.add(p);
                names.add(p.getName());
            }
        }
        io.println(ArcadeUI.YELLOW + "Please make your choice of pokemon to attack (type 1-" + pokemons.size() + ", name, or [Tab]):" + ArcadeUI.RESET);
        for (int i = 0; i < pokemons.size(); i++) {
            io.println("  " + ChoiceHelper.getBadgeForIndex(i) + " " + ArcadeUI.renderHpBar(pokemons.get(i)));
        }
        String pokemon = io.readLine("Choose pokemon (1-" + pokemons.size() + " or name):", names);
        return selectPokemon(pokemon, trainer);
    }

    @Override
    public int randomAttackByGymOwner() {
        Random rand = new Random();
        int maxAttacks = 4;
        return rand.nextInt(maxAttacks);
    }

    @Override
    public String chooseAttackPlayer(Pokemon p) {
        List<String> attacks;
        String type = p.getType();
        switch (type) {
            case "fire" -> {
                FirePokemon fp = (FirePokemon) p;
                attacks = fp.getAttacks();
            }
            case "water" -> {
                WaterPokemon wp = (WaterPokemon) p;
                attacks = wp.getAttacks();
            }
            case "electric" -> {
                ElectricPokemon ep = (ElectricPokemon) p;
                attacks = ep.getAttacks();
            }
            default -> {
                GrassPokemon gp = (GrassPokemon) p;
                attacks = gp.getAttacks();
            }
        }
        io.println("Choose your attack:");
        io.println("  " + ChoiceHelper.formatInlineOptions(attacks));
        String chosen = io.readLine("Attack (1-" + attacks.size() + " or name):", attacks);
        while (!containsIgnoreCase(attacks, chosen)) {
            io.println("Unknown attack. Please choose 1-" + attacks.size() + " or name: " + ChoiceHelper.formatInlineOptions(attacks));
            chosen = io.readLine("Attack (1-" + attacks.size() + " or name):", attacks);
        }
        return chosen;
    }

    private boolean containsIgnoreCase(List<String> list, String target) {
        if (target == null) return false;
        String trimmed = target.trim();
        for (String s : list) {
            if (s.equalsIgnoreCase(trimmed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void performAttackPlayer(Pokemon pokemon, Pokemon gymPokemon, String attack) {
        SoundEngine.playAttackSfx(pokemon.getPokemonType());
        FirePokemon fire;
        ElectricPokemon electric;
        GrassPokemon grass;
        WaterPokemon water;

        String choosenAttack = attack.toLowerCase(Locale.ROOT);

        switch (pokemon.getType()) {
            case "fire" -> {
                fire = new FirePokemon(pokemon.getName(), pokemon.getLevel(), pokemon.getHp(), pokemon.getFood(), pokemon.getSound());
                switch (choosenAttack) {
                    case "inferno" -> fire.inferno(pokemon, gymPokemon);
                    case "pyroball" -> fire.pyroBall(pokemon, gymPokemon);
                    case "firelash" -> fire.fireLash(pokemon, gymPokemon);
                    default -> fire.flameThrower(pokemon, gymPokemon);
                }
            }
            case "water" -> {
                water = new WaterPokemon(pokemon.getName(), pokemon.getLevel(), pokemon.getHp(), pokemon.getFood(), pokemon.getSound());
                switch (choosenAttack) {
                    case "surf" -> water.surf(pokemon, gymPokemon);
                    case "hydropump" -> water.hydroPump(pokemon, gymPokemon);
                    case "hydrocanon" -> water.hydroCanon(pokemon, gymPokemon);
                    default -> water.rainDance(pokemon, gymPokemon);
                }
            }
            case "grass" -> {
                grass = new GrassPokemon(pokemon.getName(), pokemon.getLevel(), pokemon.getHp(), pokemon.getFood(), pokemon.getSound());
                switch (choosenAttack) {
                    case "leafstorm" -> grass.leafStorm(pokemon, gymPokemon);
                    case "solarbeam" -> grass.solarBeam(pokemon, gymPokemon);
                    case "leechseed" -> grass.leechSeed(pokemon, gymPokemon);
                    default -> grass.leaveBlade(pokemon, gymPokemon);
                }
            }
            default -> {
                electric = new ElectricPokemon(pokemon.getName(), pokemon.getLevel(), pokemon.getHp(), pokemon.getFood(), pokemon.getSound());
                switch (choosenAttack) {
                    case "thunderpunch" -> electric.thunderPunch(pokemon, gymPokemon);
                    case "electroball" -> electric.electroBall(pokemon, gymPokemon);
                    case "thunder" -> electric.thunder(pokemon, gymPokemon);
                    default -> electric.voltTackle(pokemon, gymPokemon);
                }
            }
        }
    }

    @Override
    public void gymOwnerAttacks(Pokemon gymPokemon, Pokemon pokemon) {
        FirePokemon fire;
        ElectricPokemon electric;
        GrassPokemon grass;
        WaterPokemon water;

        switch (gymPokemon.getType()) {
            case "fire" -> {
                fire = new FirePokemon(gymPokemon.getName(), gymPokemon.getLevel(), gymPokemon.getHp(), gymPokemon.getFood(), gymPokemon.getSound());
                String attack = fire.getAttacks().get(randomAttackByGymOwner());
                switch (attack) {
                    case "inferno" -> fire.inferno(gymPokemon, pokemon);
                    case "pyroBall" -> fire.pyroBall(gymPokemon, pokemon);
                    case "fireLash" -> fire.fireLash(gymPokemon, pokemon);
                    default -> fire.flameThrower(gymPokemon, pokemon);
                }
            }
            case "water" -> {
                water = new WaterPokemon(gymPokemon.getName(), gymPokemon.getLevel(), gymPokemon.getHp(), gymPokemon.getFood(), gymPokemon.getSound());
                String attack = water.getAttacks().get(randomAttackByGymOwner());
                switch (attack) {
                    case "surf" -> water.surf(gymPokemon, pokemon);
                    case "hydroPump" -> water.hydroPump(gymPokemon, pokemon);
                    case "hydroCanon" -> water.hydroCanon(gymPokemon, pokemon);
                    default -> water.rainDance(gymPokemon, pokemon);
                }
            }
            case "grass" -> {
                grass = new GrassPokemon(gymPokemon.getName(), gymPokemon.getLevel(), gymPokemon.getHp(), gymPokemon.getFood(), gymPokemon.getSound());
                String attack = grass.getAttacks().get(randomAttackByGymOwner());
                switch (attack) {
                    case "leafStorm" -> grass.leafStorm(gymPokemon, pokemon);
                    case "solarBeam" -> grass.solarBeam(gymPokemon, pokemon);
                    case "leechSeed" -> grass.leechSeed(gymPokemon, pokemon);
                    default -> grass.leaveBlade(gymPokemon, pokemon);
                }
            }
            default -> {
                electric = new ElectricPokemon(gymPokemon.getName(), gymPokemon.getLevel(), gymPokemon.getHp(), gymPokemon.getFood(), gymPokemon.getSound());
                String attack = electric.getAttacks().get(randomAttackByGymOwner());
                switch (attack) {
                    case "thunderPunch" -> electric.thunderPunch(gymPokemon, pokemon);
                    case "electroBall" -> electric.electroBall(gymPokemon, pokemon);
                    case "thunder" -> electric.thunder(gymPokemon, pokemon);
                    default -> electric.voltTackle(gymPokemon, pokemon);
                }
            }
        }
    }

    // Bonus 2: gooi voedsel naar je eigen Pokémon in plaats van aan te vallen. Past het voedsel bij het type, dan krijgt hij +20 HP!
    private void throwFood(Pokemon pokemon) {
        List<String> foods = List.of("firenougats", "Pokeflakes", "Pokeleafs", "Pokebrocks");
        io.println(ArcadeUI.CYAN + "Choose food to throw to " + pokemon.getName() + " (match its element for +20 HP!):" + ArcadeUI.RESET);
        io.println("  " + ChoiceHelper.formatInlineOptions(foods));
        String food = io.readLine("Food (1-4 or name):", foods);

        io.println("You throw " + ArcadeUI.BOLD + food + ArcadeUI.RESET + " to " + pokemon.getName() + "!");
        if (pokemon.likes(food)) {
            pokemon.eat();
            SoundEngine.playHealJingle();
            io.println(ArcadeUI.GREEN + "✨ " + pokemon.getName() + " loves " + food + " and gains +20 HP! HP is now "
                    + pokemon.getHp() + "/" + pokemon.getMaxHp() + ArcadeUI.RESET);
        } else {
            io.println(ArcadeUI.YELLOW + "💨 " + pokemon.getName() + " sniffed at " + food
                    + " (" + pokemon.getPokemonType() + " types only eat " + pokemon.getFood() + "!). No HP gained." + ArcadeUI.RESET);
        }
    }

    @Override
    public void attackOrChange(Pokemon pokemon, Pokemon gymPokemon, PokemonTrainer trainer, PokemonGymOwner gym) {
        io.println("Do you want to attack, throw food, or change your pokemon?");
        io.println("  " + ChoiceHelper.getBadgeForIndex(0) + " [a] Attack   "
                + ChoiceHelper.getBadgeForIndex(1) + " [f] Throw Food (+20 HP)   "
                + ChoiceHelper.getBadgeForIndex(2) + " [c] Change Pokémon");
        String choice = io.readLine("Choice (1/2/3 or a/f/c):", List.of("a", "f", "c"));

        while (!choice.equalsIgnoreCase("a") && !choice.equalsIgnoreCase("f") && !choice.equalsIgnoreCase("c")) {
            io.println("Invalid choice. Please enter 1/2/3 or a/f/c.");
            choice = io.readLine("Choice (1/2/3 or a/f/c):", List.of("a", "f", "c"));
        }

        if (choice.equalsIgnoreCase("a")) {
            String attack = chooseAttackPlayer(pokemon);
            performAttackPlayer(pokemon, gymPokemon, attack);
        } else if (choice.equalsIgnoreCase("f")) {
            throwFood(pokemon);
        } else {
            // Zoals in de echte Pokémon games: wisselen kost je beurt, daarna valt Brock je nieuwe Pokémon aan!
            this.activePlayerPokemon = choosePokemon(trainer);
            ArcadeUI.playPokeballAnimation(io, ArcadeUI.GREEN, trainer.getName(), this.activePlayerPokemon);
        }
    }
}
