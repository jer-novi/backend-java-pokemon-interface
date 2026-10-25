package Pokemon;

import java.util.Arrays;
import java.util.List;

public class GrassPokemon extends Pokemon {

	private final List<String> attacks = Arrays.asList("leafStorm", "solarBeam", "leechSeed", "leaveBlade");
	private static final String TYPE = "grass";

	public GrassPokemon(String name, int level, int hp, String food, String sound) {
		super(name, level, hp, food, sound, TYPE);
	}


	public List<String> getAttacks() {
		return attacks;
	}
}
