package Pokemon;

import java.util.Arrays;
import java.util.List;

public class ElectricPokemon extends Pokemon {

	private final List<String> attacks = Arrays.asList("electroBall", "thunderPunch", "thunder", "voltTackle");
	private static final String TYPE = "electric";

	public ElectricPokemon(String name, int level, int hp, String food, String sound) {
		super(name, level, hp, food, sound, TYPE);
	}


	public List<String> getAttacks() {
		return attacks;
	}
}
