package Pokemon;

public enum PokemonType {

    FIRE("fire"),
    GRASS("grass"),
    WATER("water"),
    ELECTRIC("electric");


    private final String label;

    PokemonType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
