package Pokemon;

import java.util.Arrays;
import java.util.List;

public class GrassPokemon extends Pokemon {

    private static final String TYPE = "grass";
    private final List<String> attacks = Arrays.asList("leafStorm", "solarBeam", "leechSeed", "leaveBlade");

    public GrassPokemon(String name, int level, int hp, String food, String sound) {
        super(name, level, hp, food, sound, TYPE);
    }


    public List<String> getAttacks() {
        return attacks;
    }

    public void leafStorm(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 12, 21, 41, 31, "leafStorm");
    }

    public void solarBeam(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 16, 27, 53, 40, "solarBeam");
    }

    public void leechSeed(Pokemon name, Pokemon enemy) {
        int preAttackHp = enemy.getHp();
        AttackTypeHelper.attack(name, enemy, 6, 10, 18, 14, "leechSeed");
        int damageTransfer = preAttackHp - enemy.getHp();
        name.setHp(name.getHp() + damageTransfer);
    }

    public void leaveBlade(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 10, 17, 33, 25, "leaveBlade");

    }
}
