package Pokemon;

import java.util.Arrays;
import java.util.List;

public class WaterPokemon extends Pokemon {

    private static final String TYPE = "water";
    private final List<String> attacks = Arrays.asList("surf", "hydroPump", "hydroCanon", "rainDance");

    public WaterPokemon(String name, int level, int hp, String food, String sound) {
        super(name, level, hp, food, sound, TYPE);
    }

    public List<String> getAttacks() {
        return attacks;
    }

    public void surf(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 18, 10, 24, 32, "surf");
    }

    public void hydroPump(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 26, 14, 35, 47, "hydroPump");
    }

    public void hydroCanon(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 34, 19, 46, 60, "hydroCanon");
    }

    public void rainDance(Pokemon name, Pokemon enemy) {
        if (enemy.getType().equals("electric")) {
            System.out.println("Has no effect on " + enemy.getName());
        } else {
            AttackTypeHelper.attack(name, enemy, -10, 5, 0, 15, "rainDance");

        }
    }

}

