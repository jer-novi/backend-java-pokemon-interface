package Pokemon;

import java.util.Arrays;
import java.util.List;

public class FirePokemon extends Pokemon {

    private static final String TYPE = "fire";
    private final List<String> attacks = Arrays.asList("fireLash", "flameThrower", "pyroBall", "inferno");

    public FirePokemon(String name, int level, int hp, String food, String sound) {
        super(name, level, hp, food, sound, TYPE);
    }

    public List<String> getAttacks() {
        return attacks;
    }

    public void fireLash(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 34, 25, 18, 12, "fireLash");
    }

    public void flameThrower(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 42, 31, 22, 14, "flameThrower");
    }

    public void pyroBall(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 50, 37, 26, 16, "pyroBall");
    }

    public void inferno(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 58, 44, 31, 19, "inferno");
    }

    }

