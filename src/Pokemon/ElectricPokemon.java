package Pokemon;

import java.util.Arrays;
import java.util.List;

public class ElectricPokemon extends Pokemon {

	private static final String TYPE = "electric";
	private final List<String> attacks = Arrays.asList("electroBall", "thunderPunch", "thunder", "voltTackle");

	public ElectricPokemon(String name, int level, int hp, String food, String sound) {
		super(name, level, hp, food, sound, TYPE);
	}


	public List<String> getAttacks() {
		return attacks;
	}

    public void electroBall(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 20, 30, 10, 15,  "electroBall" );
    }
    public void thunderPunch(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 28, 42, 14, 21,  "thunderPunch" );
    }
    public void thunder(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 38, 58, -19, 29,  "thunder" );
    }
    public void voltTackle(Pokemon name, Pokemon enemy) {
        AttackTypeHelper.attack(name, enemy, 45, 68, 23, 34, "voltTackle" );
    }

}
