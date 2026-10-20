package Pokemon;

import java.util.Arrays;
import java.util.List;

public class ElectricPokemon extends Pokemon {

    private static final PokemonType TYPE = PokemonType.ELECTRIC;
    private final List<String> attacks = Arrays.asList("electroBall", "thunderPunch", "thunder", "voltTackle");

    public ElectricPokemon(String attacker, int level, int hp, String food, String sound) {
        super(attacker, level, hp, food, sound, TYPE);
    }


    @Override
    public ElectricPokemon copy() {
        return new ElectricPokemon(getName(), getLevel(), getHp(), getFood(), getSound());
    }

    public List<String> getAttacks() {
        return attacks;
    }

    public void electroBall(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 20, 30, 10, 15, "electroBall");
    }

    public void thunderPunch(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 28, 42, 14, 21, "thunderPunch");
    }

    public void thunder(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 38, 58, -19, 29, "thunder");
    }

    public void voltTackle(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 45, 68, 23, 34, "voltTackle");
    }

}
