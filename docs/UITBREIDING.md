# Uitbreiding Pokémon-gym (branch `uitbreiding`)

Stand van zaken op 2026-10-19. Niets hiervan is gecommit of gepusht.
De homeworkversie (branch `uitwerking`) blijft ongewijzigd; alles hieronder staat alleen op `uitbreiding`.

---

## 1. Wat er al gebouwd is

### 1.1 Opruimwerk dat de basis legde

| Onderdeel | Wat het doet |
|---|---|
| `PokemonType` (enum) | FIRE, GRASS, WATER, ELECTRIC met een `label`. Vervangt de losse Strings. |
| `Pokemon.getPokemonType()` | Geeft de enum terug. `getType()` (String) bestaat nog alleen voor `PokemonGymImpl`. |
| `AttackTypeHelper.attack(...)` | Rekent de aanval uit en geeft een `AttackResult` terug. **Print niets.** |
| `AttackResult` (record) | `attacker`, `target`, `attackName`, `hpChange`, `remainingHp`, `noEffect`. `hpChange` is negatief bij schade, 0 bij geen effect, positief bij een boost. |
| `AttackResult.describe()` | Geeft de console-regels als `List<String>`. Alle tekst staat op één plek. |
| `AttackTypeHelper.report(result)` | Het **enige** punt waar een aanvalsresultaat nog naar `System.out` gaat. |
| `leechSeed` | Gebruikt `result.damageDealt()` in plaats van hp zelf te vergelijken. |
| `rainDance` | Gebruikt de enum en een `AttackResult.noEffect(...)`. |

### 1.2 Bonusopdrachten uit de README

**Bonus 1: voer geven** (`Pokemon.java`)
- `eat()` geeft +20 hp (constante `FOOD_BOOST`).
- `likes(String food)` controleert of het voedsel past. Vergelijking is niet hoofdlettergevoelig, en Ditto (`"Everything"`) eet alles.
- Het veld `food` bestond al.

**Bonus 2: voedsel gooien** (`PokemonGymImpl.attackOrChange` en `throwFood`)
- Nieuwe keuze `f` naast `a` (aanvallen) en `c` (wisselen).
- Je typt een voedselnaam en gooit die naar de **tegenstander**. Past het, dan krijgt de tegenstander een hp-boost. Past het niet, dan gebeurt er niets.
- Ontwerpkeuze: de README zegt niet duidelijk naar wie je gooit. Wil je het naar je eigen Pokémon laten gaan, dan verander je in `throwFood` alleen het `target`.

**Bonus 3: dode Pokémon blijven dood** (`Pokemon.isFainted()`, `Pokemon.copy()`, `PokemonGymImpl`)
- `copy()` is abstract in `Pokemon` en wordt in elke subklasse geïmplementeerd. Zo krijgt Brock **eigen kopieën** (aangemaakt in de constructor van `PokemonGymImpl`, veld `gymPokemons`). Eerst deelden jij en Brock dezelfde objecten, dus een dode Pokémon was ook voor Brock dood.
- `selectPokemon` kiest alleen Pokémon met hp > 0. Bij een typefout krijg je de eerste die nog kan vechten.
- In `fightRound` valt een Pokémon niet meer aan nadat hij door de tegenstander is verslagen.
- `enteredTheGym` controleert of jij of Brock nog Pokémon hebt. Zo niet: melding en stoppen, geen crash meer op `rand.nextInt(0)`.

### 1.3 Gewijzigde bestanden

- `src/Pokemon/`: `Pokemon`, `PokemonType` (nieuw), `AttackResult` (nieuw), `AttackTypeHelper`, `FirePokemon`, `WaterPokemon`, `GrassPokemon`, `ElectricPokemon`.
- `src/PokemonGymImpl.java` (bonussen 2 en 3). Dit is het NOVI-bestand; voor de homework mocht het niet veranderen.
- `pom.xml`, `.gitignore`.
- Tijdelijke testklassen: `src/TestFireLash.java`, `src/TestRainDance.java`, `src/TestBonus.java`. Die verdwijnen zodra de JUnit-tests er staan.

### 1.4 Wat getest is, en wat niet

- Getest met de testklassen: alle `rainDance`-scenario's, `leechSeed`, `eat`, `likes`, `isFainted`, `selectPokemon`, `copy`.
- **Niet** automatisch getest: een compleet spel. Gepijpte invoer werkt niet omdat elke methode een eigen `Scanner(System.in)` maakt, die alle invoer in één keer opslokt. Dit lost de I/O-interface uit hoofdstuk 2 op.

---

## 2. JLine: wat er nog nodig is

### 2.1 Het idee

JLine vervangt `Scanner` + `System.out` door een echte terminal-bibliotheek: pijltjestoetsen, geschiedenis en tab-aanvulling (bijvoorbeeld op aanvalsnamen en Pokémon-namen).

Het scharnierpunt is al klaar: aanvalsresultaten gaan via `AttackTypeHelper.report(...)`. Wat nog ontbreekt is **één** plek voor alle invoer en uitvoer. Zonder die plek kun je JLine niet inpluggen en kun je ook geen spel automatisch testen.

### 2.2 Stappen

**Stap 1: interface `GameIO`** (eigen bestand, bijvoorbeeld in `src/`):

```java
public interface GameIO {
    void println(String text);
    String readLine(String prompt);
    // later eventueel: String choose(String prompt, List<String> options);
}
```

**Stap 2: twee implementaties**
- `ConsoleIO`: gebruikt één gedeelde `Scanner` en `System.out`. Gedrag blijft gelijk aan nu.
- `JLineIO`: gebruikt JLine (zie 2.3).
- `FakeIO` (in de tests): geeft een vooraf ingestelde rij antwoorden terug en bewaart alles wat geprint wordt. Hiermee kun je een heel spel testen.

**Stap 3: `GameIO` doorgeven (dependency injection)**
- `PokemonGymImpl` krijgt een extra constructor-parameter: `new PokemonGymImpl(pokemons, io)`.
- Alle `System.out.println(...)` in `PokemonGymImpl` worden `io.println(...)`.
- Alle `new Scanner(System.in)` verdwijnen. Er zijn er 5: in `fightRound`, `choosePokemon`, `chooseAttackPlayer`, `attackOrChange` en in `Main`. Ze worden `io.readLine("...")`.
- `AttackTypeHelper.report(result)` moet ook bij `io` kunnen. Twee opties:
  - de subklassen krijgen een `GameIO` mee, of
  - de attack-methodes geven de `AttackResult` terug en `PokemonGymImpl` print die. Dit is netter, maar de 16 methodes veranderen dan van `void` naar `AttackResult`. Dat kan op deze branch.
- Tip: gebruik in IntelliJ **Alt+Enter** op `System.out.println` en de refactoring *Replace* met een regex, of doe het handmatig per bestand. Check daarna met `Ctrl+Shift+F` dat er geen `System.out` of `Scanner` meer staat buiten `ConsoleIO`.

**Stap 4: `Main` aanpassen**
- Maak `GameIO io = new JLineIO();` (of `ConsoleIO`) en geef die aan `PokemonGymImpl`.
- Gebruik `io` ook voor de naamvraag en de ja/nee-vragen in `Main`.
- Sluit de terminal netjes af (`try-with-resources`).

**Stap 5: afhankelijkheid in `pom.xml`**

```xml
<dependencies>
    <dependency>
        <groupId>org.jline</groupId>
        <artifactId>jline</artifactId>
        <version>4.4.8</version>
    </dependency>
</dependencies>
```

Controleer de nieuwste versie op Maven Central voor je dit definitief maakt. Draai daarna `mvn -q compile` om te kijken of het binnenkomt.

### 2.3 Een eerste `JLineIO`

**Hoe zeker is deze code?** Gecontroleerd via Context7 (docs van JLine 3.x), de officiële site jline.org en de release notes. Voor 4.x zelf staat er op Context7 bijna niets. Ik heb de code **niet gecompileerd** tegen 4.4.8, omdat de dependency er nog niet in zit. Zodra je hem in de pom zet, laat IntelliJ meteen zien of een import niet klopt.

Wat wel bevestigd is:
- Maven-coördinaten: `org.jline:jline` (jline.org toont 4.4.3 als voorbeeld, jij gebruikt 4.4.8).
- 4.x vraagt minimaal **Java 11**. Jij zit op 25, dus geen enkel probleem.
- De packages blijven `org.jline.*` (de FFM-provider heet `org.jline.terminal.impl.ffm`). Een officiële migratiegids van 3 naar 4 heb ik niet gevonden. Staat er iets anders in je IDE, vertrouw dan de IDE.

```java
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class JLineIO implements GameIO, AutoCloseable {
    private final Terminal terminal;
    private final LineReader reader;

    public JLineIO() throws IOException {
        terminal = TerminalBuilder.builder()
                .system(true)
                .dumb(true)   // val terug op een simpele terminal, bijvoorbeeld in het Run-venster van IntelliJ
                .build();
        reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("inferno", "pyroBall", "fireLash", "flameThrower"))
                .build();
    }

    @Override
    public void println(String text) {
        terminal.writer().println(text);
        terminal.flush();
    }

    @Override
    public String readLine(String prompt) {
        try {
            return reader.readLine(prompt + " ");
        } catch (UserInterruptException | EndOfFileException e) {
            // Ctrl+C of Ctrl+D: het spel netjes stoppen in plaats van een stacktrace
            throw new GameQuitException();   // eigen kleine RuntimeException, vang je op in Main
        }
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }
}
```

Let op `GameQuitException`: dat is een eigen klasse die je zelf nog moet maken (`class GameQuitException extends RuntimeException {}`). Je vangt hem op in `Main` en print daar "Thank you for playing".

Latere verbetering: de `StringsCompleter` per keuze instellen (aanvalsnamen van de huidige Pokémon, Pokémon-namen, `a`/`f`/`c`). Dat kan met een `readLine(prompt, options)`-variant in `GameIO`. Zie ook 2.6.

### 2.4 Valkuilen

- **IntelliJ Run-venster is geen echte terminal.** JLine valt daar terug op een eenvoudige "dumb" terminal (geen pijltjestoetsen of tab). Draai het spel in de Terminal van IntelliJ of in Windows Terminal. Zet anders in de Run-configuratie *Emulate terminal in output console* aan.
- **ANSI-kleuren** uit `Main` (`ANSI_RED` enzovoort) werken in een echte terminal. In een dumb terminal zie je losse tekens.
- **Eén terminal per programma.** Maak `JLineIO` één keer aan en geef hem door. Niet meerdere keren.
- Het uitvoerbare programma: `mvn -q compile` en daarna `java -cp target/classes:<pad naar jline.jar> Main`, of voeg de `exec-maven-plugin` toe. Met `mvn package` krijg je alleen een jar zonder JLine erin, tenzij je een shade/assembly-plugin gebruikt.

### 2.5 Volgorde van werken (zie ook 2.6 voor extra's)

1. `GameIO` + `ConsoleIO` + injectie. Het spel gedraagt zich dan **identiek** aan nu. Dit is het veilige tussenpunt (hier mag je committen).
2. `FakeIO` en een automatische test die een heel spel doorspeelt.
3. `JLineIO` inpluggen en `Main` omzetten.
4. Aanvulling met tab (completer).

### 2.6 Wat JLine 3 en 4.x bieden dat leuk is voor deze game

Bronnen: Context7 (JLine 3-docs), jline.org en de release notes op GitHub (`jline/jline3`). Wat uit welke bron komt, staat erbij. Dingen die ik niet kon bevestigen, zijn als zodanig gemarkeerd.

#### Nieuw of anders in 4.x (relevant voor jouw setup)

| Wijziging | Wat het voor jou betekent |
|---|---|
| Minimaal Java 11 (4.0.0) | Geen probleem, je zit op 25. |
| JNA-provider verwijderd; alleen JNI en **FFM** blijven (4.0.0) | Op Java 25 gebruikt JLine automatisch FFM. De `jdk11`-classifier die jline.org noemt, is alleen voor Java 11-21 en hoef je dus **niet** te gebruiken. Het kan zijn dat Java 25 een waarschuwing toont over native toegang; dan helpt de JVM-vlag `--enable-native-access=ALL-UNNAMED`. Dat is een inschatting, geen bevestigd feit uit de docs. |
| Een `Terminal` gooit een exception als je hem na `close()` nog gebruikt (4.0.0) | Maak de terminal **één keer** aan in `Main` en sluit hem helemaal aan het eind. Dit past bij de `GameIO`-opzet uit 2.2. |
| Providers via standaard Java `ServiceLoader` (4.0.0) | Geen actie nodig. Alleen relevant als je later een gebundelde jar maakt: dan moeten de service-bestanden behouden blijven. |
| Wachttijd bij dubbelzinnige toetsen van 1000 ms naar **100 ms** (4.4.0, breaking) | Esc en toetsencombinaties voelen sneller. Je hoeft niets te doen. |
| Veel fixes in 4.4.1-4.4.8 (kleurpalet, Windows-muis, tab-aanvulling, hangende terminal-probe, ReDoS in history) | Alleen een reden om gewoon op 4.4.8 te blijven. Geen van de fixes vraagt om code. |
| Synchronized output en in-band resize (4.4.0) | Niet nodig voor een tekstspel. Pas interessant als je ooit een scherm met een hp-balk laat bijwerken zonder flikkering. |
| Kitty keyboard protocol, musl-ondersteuning, `JdkConsoleProvider` (4.4.0) | Niet relevant voor deze game. |

Niet bevestigd: of er in 4.x een compleet nieuwe prompt-API, thema's of nieuwe widgets zijn. De pagina's die ik kon lezen noemen dat niet. Kijk op jline.org/docs als je dat wilt uitzoeken.

#### Handig uit JLine 3 (werkt ook in 4.x, volgens de documentatie)

1. **Tab-aanvulling voor aanvallen en Pokémon.** `StringsCompleter` met vaste lijsten is de eenvoudigste vorm. Een eigen `Completer` per beurt laat alleen de aanvallen van de Pokémon zien die op dat moment vecht (`fp.getAttacks()`).
2. **Ctrl+C en Ctrl+D afvangen.** `UserInterruptException` (Ctrl+C) en `EndOfFileException` (Ctrl+D) gooit JLine zelf. Vang ze op, zoals in de code van 2.3, zodat het spel netjes sluit.
3. **Kleur met `AttributedStringBuilder` en `AttributedStyle`** in plaats van de losse `ANSI_RED`-constanten uit `Main`. Voorbeeld uit de docs:
   `AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)`.
   Zo werkt kleur ook beter op terminals die geen ANSI aankunnen. Handig voor groene hp-boosts en rode schade.
4. **`Highlighter`**: kleurt wat je typt terwijl je typt. Voor de game: een geldige aanval wordt groen, een onbekende rood. Dat sluit mooi aan bij de typefout-afhandeling in `selectPokemon`.
5. **Geschiedenis** (pijltje omhoog): staat standaard aan in de `LineReader`. Handig om dezelfde aanval snel te herhalen.
6. **`.dumb(true)`** bij `TerminalBuilder` voor een veilige terugval, bijvoorbeeld in het Run-venster van IntelliJ (staat in de code van 2.3).

#### Aanbevolen aanpak

Begin klein: `JLineIO` met `StringsCompleter` en de twee exception-afhandelingen. Voeg kleur en highlighter pas toe als het spel er netjes mee speelt. Test het spel in een echte terminal, niet in het Run-venster (zie 2.4).

---

## 3. JUnit-tests: zo schrijf je ze zelf

### 3.1 Eerst: pom.xml

Je broncode staat in `src` (`<sourceDirectory>src</sourceDirectory>`). Zet de tests daarom **niet** in `src`, anders worden ze mee gecompileerd als gewone code. Voeg toe:

```xml
<build>
    <sourceDirectory>src</sourceDirectory>
    <testSourceDirectory>test</testSourceDirectory>
</build>

<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.4</version>   <!-- controleer de nieuwste 5.x op Maven Central -->
        <scope>test</scope>
    </dependency>
</dependencies>
```

Zorg dat de `maven-surefire-plugin` versie 3.x is (nodig voor JUnit 5), bijvoorbeeld door hem zonder versie te laten staan: Maven 3.9 gebruikt dan een recente. Draaien kan met `mvn test`.

Maak de map `test/Pokemon/` aan. Zet je testklassen **in het package `Pokemon`**, want `AttackTypeHelper` en `AttackResult` zijn package-private. In IntelliJ markeer je `test` als *Test Sources Root* (rechtermuisknop → Mark Directory as).

### 3.2 Een eerste test, stap voor stap

`test/Pokemon/AttackTypeHelperTest.java`:

```java
package Pokemon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackTypeHelperTest {

    @Test
    void rainDanceBoostsGrass() {
        // Arrange: bouw de situatie op
        WaterPokemon blastoise = new WaterPokemon("Blastoise", 40, 110, "Pokeflakes", "Blaaa");
        GrassPokemon venusaur  = new GrassPokemon("Venusaur", 50, 100, "Pokeleafs", "Veee");

        // Act: doe één ding
        AttackResult result = AttackTypeHelper.attack(blastoise, venusaur, -10, 5, 0, 15, "rainDance");

        // Assert: controleer de uitkomst
        assertEquals(110, venusaur.getHp());
        assertEquals(10, result.hpChange());
    }
}
```

Elke test heeft dus drie delen: **Arrange** (voorbereiden), **Act** (één actie), **Assert** (controleren). Houd één onderwerp per test.

Handige assert-methodes: `assertEquals(verwacht, werkelijk)` (let op de volgorde), `assertTrue`, `assertFalse`, `assertNotSame`.

### 3.3 Testlijst om zelf te schrijven

Begin met wat je al kent uit de testklassen. Zet elk scenario om in één `@Test`:

| Test | Verwacht |
|---|---|
| rainDance tegen GRASS (hp 100) | hp 110, `hpChange` = +10 |
| rainDance tegen ELECTRIC | hp 100, `noEffect` is true |
| rainDance tegen FIRE | hp 85 |
| rainDance tegen WATER | hp 95 |
| rainDance, vijand hp 5, schade is 5 | hp 0 |
| aanval die meer schade doet dan de hp | hp 0 (nooit negatief), `hpChange` = min(schade, hp) |
| `leechSeed`: vijand 100, jij 100 | vijand 90, jij 110 (let op `kopie`, zie hieronder) |
| `eat()` | hp +20 |
| `likes("pizza")` / `likes("Everything")` bij Ditto | false / true |
| `isFainted()` bij hp 0 | true |
| `copy()` | wijzigen van de kopie verandert het origineel niet |
| `setHp(-5)` | hp blijft 0 |

Let bij `leechSeed` op wat `PokemonGymImpl` doet: de methode wordt op een **kopie** aangeroepen, de echte Pokémon staan in de parameters (`attacker`, `enemy`). Test dus zoals het spel het doet, bijvoorbeeld `kopie.leechSeed(venusaur, gyarados)`.

### 3.4 Tips

- Gebruik `@BeforeEach` als meerdere tests dezelfde Pokémon nodig hebben. Hij draait vóór elke test, zodat tests elkaar niet beïnvloeden.
- Voor meerdere waarden in één test: `@ParameterizedTest` met `@CsvSource` (dan heb je ook de dependency `junit-jupiter-params`, die zit al in `junit-jupiter`).
- `mvn test` draait alles. In IntelliJ klik je op het groene pijltje naast een test.
- Eerst laten falen: pas een verwachte waarde tijdelijk aan en kijk of de test rood wordt. Zo weet je dat hij echt iets controleert.

### 3.5 Wat er nog nodig is voor de tests

1. `pom.xml` aanpassen zoals in 3.1.
2. Map `test/Pokemon/` aanmaken en markeren als Test Sources Root.
3. De tests uit 3.3 schrijven.
4. Daarna `src/TestFireLash.java`, `src/TestRainDance.java` en `src/TestBonus.java` verwijderen.
5. Later, met `FakeIO`: een test die een heel spel doorspeelt (zie 2.5, stap 2).

---

## 4. Openstaande punten (los van JLine en tests)

- [ ] **`target/` uit git**: regel `target` in `.gitignore` en `git rm -r --cached target` (volgens `git status` is dit deels al gebeurd; controleren).
- [ ] **Tabs**: `PokemonGym.java`, `PokemonGymOwner.java`, `PokemonTrainer.java`, `ElectricPokemon.java` bevatten nog tab-inspringing. Edit → Convert Indents → To Spaces.
- [ ] **Parameter `name` → `attacker`** in de 16 aanvalsmethodes en in de helper (Shift+F6 per parameter, **zonder** getters/setters mee te hernoemen).
- [ ] **`getType()` verwijderen** zodra `PokemonGymImpl` de enum gebruikt (3 plekken, nu nog `switch` op Strings).
- [ ] **`pom.xml`**: `<name>REST Service Parent</name>` en `<description>` zijn restanten van een tutorial.
- [ ] **Ontwerpvraag** aan je mentor: mag `AttackTypeHelper` een aparte klasse zijn, gezien de README?
- [ ] **Bekende spelfout** in `attackOrChange`: bij `c` (wisselen) roept de code zichzelf recursief aan en daarna `fightRound`. Dat werkt maar is rommelig. Opruimen kan zodra de invoer via `GameIO` loopt.
- [ ] **Eerst committen** (alleen als jij het wilt): er staat veel werk ongecommit op `uitbreiding`.
