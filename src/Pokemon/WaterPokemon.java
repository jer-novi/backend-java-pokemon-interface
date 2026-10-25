package Pokemon;

import java.util.Arrays;
import java.util.List;

public class WaterPokemon extends Pokemon {

	private final List<String> attacks = Arrays.asList("surf", "hydroPump", "hydroCanon", "rainDance");
	private static final String TYPE = "water";

	public WaterPokemon(String name, int level, int hp, String food, String sound) {
		super(name, level, hp, food, sound, TYPE);
	}


	public List<String> getAttacks() {
		return attacks;
	}
}
