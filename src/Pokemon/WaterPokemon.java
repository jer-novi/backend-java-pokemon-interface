package Pokemon;

import java.util.Arrays;
import java.util.List;

public class WaterPokemon extends Pokemon {

    private static final PokemonType TYPE = PokemonType.WATER;
    private final List<String> attacks = Arrays.asList("surf", "hydroPump", "hydroCanon", "rainDance");

    public WaterPokemon(String attacker, int level, int hp, String food, String sound) {
        super(attacker, level, hp, food, sound, TYPE);
    }

    @Override
    public WaterPokemon copy() {
        return new WaterPokemon(getName(), getLevel(), getHp(), getFood(), getSound());
    }

    public List<String> getAttacks() {
        return attacks;
    }

    public void surf(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 18, 10, 24, 32, "surf");
    }

    public void hydroPump(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 26, 14, 35, 47, "hydroPump");
    }

    public void hydroCanon(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 34, 19, 46, 60, "hydroCanon");
    }

    public void rainDance(Pokemon name, Pokemon enemy) {
        if (enemy.getPokemonType() == PokemonType.ELECTRIC) {
            AttackTypeHelper.report(AttackResult.noEffect(name, enemy, "rainDance"));
        } else {
            AttackTypeHelper.attackAndReport(name, enemy, -10, 5, 0, 15, "rainDance");

        }
    }

}

