package Pokemon;

public abstract class Pokemon {
	private final String name;
	private final int level;
	private int hp;

	private final String food;
	private final String sound;

	private final String type;


	Pokemon(String name, int level, int hp, String food, String sound, String type) {
		this.name = name;
		this.hp = hp;
		this.level = level;
		this.food = food;
		this.sound = sound;
		this.type= type;
	}


	public int getHp() {
		return hp;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}



	public String getName() {
		return name;
	}

	public int getLevel() {
		return level;
	}

	public String getFood() {
		return food;
	}

	public String getSound() {
		return sound;
	}

	public String getType() {
		return type;
	}

}