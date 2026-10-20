package Pokemon;

public abstract class Pokemon {
    private final String name;
    private final int level;
    private final String food;
    private final String sound;
    private final PokemonType type;
    private final int maxHp;
    private int hp;


    Pokemon(String name, int level, int hp, String food, String sound, PokemonType type) {
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
        this.level = level;
        this.food = food;
        this.sound = sound;
        this.type = type;
    }

    private static final int FOOD_BOOST = 20;

    /** Maakt een onafhankelijke kopie (zelfde gegevens, eigen hp). */
    public abstract Pokemon copy();

    /** Bonus 1: de Pokémon eet en krijgt een hp-boost. */
    public void eat() {
        setHp(hp + FOOD_BOOST);
    }

    /** Past dit voedsel bij deze Pokémon? Ditto eet alles. */
    public boolean likes(String offeredFood) {
        return food.equalsIgnoreCase(offeredFood) || food.equalsIgnoreCase("Everything");
    }

    /** Bonus 3: een Pokémon zonder hp kan niet meer gebruikt worden. */
    public boolean isFainted() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, hp);
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

    public PokemonType getPokemonType() {
        return type;

    }

    // Deze String-getter houdt PokemonGymImpl compatibel.
    public String getType() {
        return type.getLabel();

    }


}

