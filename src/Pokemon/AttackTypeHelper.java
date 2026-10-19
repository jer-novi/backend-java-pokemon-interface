package Pokemon;

final class AttackTypeHelper {
    static void attack(Pokemon name, Pokemon enemy, int damageGrass, int damageWater, int damageElectric, int damageFire, String attackName) {
        System.out.println(name.getName() + " attacks " + enemy.getName() + " with " + attackName);
        int damage = 0;
        int preAttackHp = enemy.getHp();
        switch (enemy.getType()) {
            case "grass" -> damage = damageGrass;

            case "water" -> damage = damageWater;

            case "electric" -> damage = damageElectric;

            case "fire" -> damage = damageFire;
        }
        if (damage > 0) {
            damage = Math.min(preAttackHp, damage);
            System.out.println(enemy.getName() + " loses " + damage + " hp");
            enemy.setHp(enemy.getHp() - damage);
        } else if (damage == 0) {
            System.out.println(enemy.getName() + " is not affected by  " + attackName );
        } else {
            int boost = Math.abs(damage);
            System.out.println(enemy.getName() + " gains " + boost + " hp");
            enemy.setHp(enemy.getHp() + boost);
        }
        System.out.println(enemy.getName() + "'s remaining hp is " + enemy.getHp());

    }
}