package Pokemon;

final class AttackTypeHelper {

    /** Voert de aanval uit en geeft het resultaat terug. Print niets. */
    static AttackResult attack(Pokemon name, Pokemon enemy, int damageGrass, int damageWater, int damageElectric, int damageFire, String attackName) {
        int preAttackHp = enemy.getHp();

        int damage = switch (enemy.getPokemonType()) {
            case GRASS -> damageGrass;
            case WATER -> damageWater;
            case ELECTRIC -> damageElectric;
            case FIRE -> damageFire;
        };

        // negatieve damage = boost
        enemy.setHp(preAttackHp - damage);

        int hpChange = enemy.getHp() - preAttackHp;
        return new AttackResult(name.getName(), enemy.getName(), attackName, hpChange, enemy.getHp(), false);
    }

    /** Voert de aanval uit en print het resultaat. Tijdelijk, tot er een I/O-interface is. */
    static AttackResult attackAndReport(Pokemon name, Pokemon enemy, int damageGrass, int damageWater, int damageElectric, int damageFire, String attackName) {
        AttackResult result = attack(name, enemy, damageGrass, damageWater, damageElectric, damageFire, attackName);
        report(result);
        return result;
    }

    /** Print een resultaat. Tijdelijk, tot er een I/O-interface is. */
    static void report(AttackResult result) {
        result.describe().forEach(System.out::println);
    }
}
