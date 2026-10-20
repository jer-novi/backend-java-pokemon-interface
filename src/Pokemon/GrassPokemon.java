package Pokemon;

import java.util.Arrays;
import java.util.List;

public class GrassPokemon extends Pokemon {

    private static final PokemonType TYPE = PokemonType.GRASS;
    private final List<String> attacks = Arrays.asList("leafStorm", "solarBeam", "leechSeed", "leaveBlade");

    public GrassPokemon(String attacker, int level, int hp, String food, String sound) {
        super(attacker, level, hp, food, sound, TYPE);
    }


    @Override
    public GrassPokemon copy() {
        return new GrassPokemon(getName(), getLevel(), getHp(), getFood(), getSound());
    }

    public List<String> getAttacks() {
        return attacks;
    }

    public void leafStorm(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 12, 21, 41, 31, "leafStorm");
    }

    public void solarBeam(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 16, 27, 53, 40, "solarBeam");
    }

    public void leechSeed(Pokemon name, Pokemon enemy) {
        AttackResult result = AttackTypeHelper.attackAndReport(name, enemy, 6, 10, 18, 14, "leechSeed");
        name.setHp(name.getHp() + result.damageDealt());
    }

    public void leaveBlade(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attackAndReport(name, enemy, 10, 17, 33, 25, "leaveBlade");

    }
}
