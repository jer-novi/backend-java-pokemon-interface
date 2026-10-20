package Pokemon;

import java.util.List;

/**
 * Het resultaat van één aanval. Bevat alleen gegevens, geen console-uitvoer.
 * hpChange is negatief bij schade, 0 bij geen effect en positief bij een boost.
 * noEffect is true als de aanval helemaal niet wordt uitgevoerd (bv. rainDance tegen electric).
 */
record AttackResult(String attacker, String target, String attackName, int hpChange, int remainingHp, boolean noEffect) {

    static AttackResult noEffect(Pokemon attacker, Pokemon target, String attackName) {
        return new AttackResult(attacker.getName(), target.getName(), attackName, 0, target.getHp(), true);
    }

    int damageDealt() {
        return Math.max(0, -hpChange);
    }

    List<String> describe() {
        String reset = "\u001B[0m";
        String cyan = "\u001B[36m";
        String red = "\u001B[31m";
        String green = "\u001B[32m";
        String yellow = "\u001B[33m";
        String bold = "\u001B[1m";

        String paddedAttacker = String.format("%-11s", attacker);
        String paddedTarget = String.format("%-11s", target);
        String paddedAttack = String.format("%-13s", "[" + attackName + "]");

        if (noEffect) {
            return List.of(
                    cyan + "  >> " + bold + paddedAttacker + reset + cyan + " uses " + paddedAttack + " -> " + bold + paddedTarget + reset,
                    yellow + "  >> No effect on " + target + "!" + reset
            );
        }
        String effect;
        if (hpChange < 0) {
            int dmg = -hpChange;
            String bell = dmg >= 40 ? "\007" : "";
            String crit = dmg >= 40 ? bold + yellow + " [SUPER HIT!]" + reset : "";
            effect = bell + red + String.format("  >> %-11s loses   %3d HP!", target, dmg) + reset + crit;
        } else if (hpChange == 0) {
            effect = yellow + String.format("  >> %-11s takes     0 damage (unaffected)", target) + reset;
        } else {
            effect = green + String.format("  >> %-11s gains  +%3d HP!", target, hpChange) + reset;
        }
        return List.of(
                cyan + "  >> " + bold + paddedAttacker + reset + cyan + " uses " + paddedAttack + " on " + bold + paddedTarget + reset,
                effect,
                String.format("  >> %-11s HP now: %s%3d HP%s", target, bold, remainingHp, reset)
        );
    }
}
