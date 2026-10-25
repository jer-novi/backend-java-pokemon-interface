package Pokemon;

import java.util.Arrays;
import java.util.List;

public class FirePokemon extends Pokemon {

	private final List<String> attacks = Arrays.asList("fireLash", "flameThrower", "pyroBall", "inferno");
	private static final String TYPE = "fire";

	public FirePokemon(String name, int level, int hp, String food, String sound) {
		super(name, level, hp, food, sound, TYPE);
	}


	public List<String> getAttacks() {
		return attacks;
	}
}
