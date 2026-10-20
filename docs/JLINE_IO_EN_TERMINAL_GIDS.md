# De Grote Pokémon Terminal & JLine Gids (Voor Beginners)

Welkom! In dit document vind je een complete, kristalheldere uitleg van alles wat we zojuist hebben gebouwd in het project (op de nieuwe branch `feature/jline-io`). 

Geen ingewikkeld academisch jargon, maar duidelijke metaforen, stap-voor-stap uitleg en praktische voorbeelden.

---

## Inhoudsopgave
1. [In het kort: Wat was het probleem en wat is er opgelost?](#1-in-het-kort-wat-was-het-probleem-en-wat-is-er-opgelost)
2. [Kernconcepten uitgelegd alsof je net begint met Java](#2-kernconcepten-uitgelegd-alsof-je-net-begint-met-java)
   - 2.1 [Wat is een Interface (`GameIO`)? — *De Menukaart*](#21-wat-is-een-interface-gameio--de-menukaart)
   - 2.2 [Wat is Dependency Injection? — *De Stekkerdoos*](#22-wat-is-dependency-injection--de-stekkerdoos)
   - 2.3 [Exceptions & `GameQuitException` — *De Noodrem van de Trein*](#23-exceptions--gamequitexception--de-noodrem-van-de-trein)
   - 2.4 [Wat is Typecasting (`LineReaderImpl`)? — *De Vermomming*](#24-wat-is-typecasting-linereaderimpl--de-vermomming)
   - 2.5 [Run-venster vs Echte Terminal (`wt`) — *Bioscoop vs Storyboard*](#25-run-venster-vs-echte-terminal-wt--bioscoop-vs-storyboard)
   - 2.6 [Het Mysterie van de "Ling-Ling" Bel & de `Supplier` (Lambda)](#26-het-mysterie-van-de-ling-ling-bel--de-supplier-lambda)
   - 2.7 [De Onzichtbare Spatie (`.trim()`)](#27-de-onzichtbare-spatie-trim)
   - 2.8 [Validatielussen met `while` — *De Portier aan de Deur*](#28-validatielussen-met-while--de-portier-aan-de-deur)
3. [Overzicht van alle aangepaste en nieuwe bestanden](#3-overzicht-van-alle-aangepaste-en-nieuwe-bestanden)
4. [Hoe speel je de game nu?](#4-hoe-speel-je-de-game-nu)
5. [Brainstorm: Hoe maken we deze game nóg vetter in de terminal?](#5-brainstorm-hoe-maken-we-deze-game-nóg-vetter-in-de-terminal)

---

## 1. In het kort: Wat was het probleem en wat is er opgelost?

### Vóór vanavond:
- Overal in de code stond `new Scanner(System.in)` en `System.out.println(...)`.
- Als je een typefout maakte (bijvoorbeeld `h` in plaats van `yes`), stopte het spel direct (`exit code 0`).
- Er was geen tab-aanvulling (autocompletion); je moest elke aanvalsnaam letter voor letter exact goed typen (inclusief hoofdletters zoals `electroBall`).
- Als je op `Ctrl+C` drukte om te stoppen, kreeg je een lelijke crash met rode foutmeldingen (stacktrace).

### Na vanavond:
- Er is **één centrale plek voor invoer en uitvoer**: de interface `GameIO`.
- **JLine 4.4.8** is geïntegreerd: een professionele terminal-bibliotheek die Tab-aanvulling, geschiedenis (pijltje omhoog) en toetsenbord-events beheert.
- **Dynamische Tab-aanvulling**:
  - Bij `yes/no` vragen: Tab vult aan naar `yes` of `no`.
  - Bij Pokémon kiezen: Tab vult alleen jouw levende Pokémon aan.
  - Bij actiekeuze: Tab vult `a`, `f`, of `c` aan.
  - Bij aanvallen: Tab vult **alleen de 4 aanvallen van jouw huidige vechtende Pokémon** aan!
- **Nette afsluiting**: Druk op `Ctrl+C` en het spel zegt beleefd: *"Thank you for playing"* via `GameQuitException`.
- **Fouttolerante invoer**: Typfouten crashen het spel niet meer; een `while`-lus vraagt netjes opnieuw tot er een geldige keuze is.

---

## 2. Kernconcepten uitgelegd alsof je net begint met Java

### 2.1 Wat is een Interface (`GameIO`)? — *De Menukaart*
Stel je gaat naar een restaurant. Op de menukaart staat: *"Koffie"*.
Jij weet: als ik koffie bestel, krijg ik een warme drank. Hoe de barista die koffie precies zet (met een bonenmachine, een filter, of oploskoffie), maakt voor jou niet uit.

Een **Interface** in Java is precies zo'n menukaart:
```java
public interface GameIO {
    void println(String text);
    String readLine(String prompt);
    String readLine(String prompt, List<String> options);
}
```
Het zegt alleen: *"Wie een `GameIO` is, móét deze 3 dingen kunnen."*
- `JLineIO` doet het met de luxe JLine-terminal (met tab-toetsen).
- `ConsoleIO` doet het simpelweg met `Scanner` en `System.out`.
- `FakeIO` (voor tests) doet het met vooraf ingevulde antwoorden.

De rest van je spel (`PokemonGymImpl`) hoeft niet meer te weten wát voor terminal er aanhangt; hij praat alleen nog tegen de menukaart!

---

### 2.2 Wat is Dependency Injection? — *De Stekkerdoos*
Vroeger maakte `PokemonGymImpl` binnenin zijn eigen methodes steeds een nieuwe `new Scanner(System.in)` aan. Dat is alsof je in een lamp zélf een batterij vastsoldeert. Als de batterij leeg is, moet je de hele lamp slopen.

**Dependency Injection** betekent: *"Bouw een stekker aan het apparaat en stop de stroom er van buitenaf in."*

```java
// We geven 'io' mee aan de constructor van de gym:
public PokemonGymImpl(List<Pokemon> pokemons, GameIO io) {
    this.pokemons = pokemons;
    this.io = io; // <-- De gym gebruikt nu déze I/O!
}
```
In `Main.java` pluggen we `JLineIO` in:
```java
JLineIO io = new JLineIO();
PokemonGymImpl pokemonGym = new PokemonGymImpl(pokemons, io);
```
Hierdoor kun je later zonder één regel aan `PokemonGymImpl` te veranderen wisselen van terminal!

---

### 2.3 Exceptions & `GameQuitException` — *De Noodrem van de Trein*
Wat gebeurt er als een reiziger aan de noodrem trekt in wagon 8 van een rijdende trein?
De machinist hoeft niet in wagon 1, 2, 3, 4, 5, 6 en 7 te controleren of er geremd moet worden: de rem trekt de hele trein in één keer tot stilstand.

#### Waarom `RuntimeException` (Unchecked)?
In Java zijn er twee soorten fouten:
1. **Checked (`extends Exception`)**: Java dwingt je om bij álles wat je doet een formulier in te vullen (`throws Exception`). Als `readLine` een checked exception zou gooien, zou je in wel 15 methodes `throws Exception` moeten bijtypen.
2. **Unchecked (`extends RuntimeException`)**: Dit is de noodrem. Je hoeft niets te declareren.

```java
public class GameQuitException extends RuntimeException {
}
```
Zodra de speler op `Ctrl+C` drukt, vangt JLine dat af en gooit `throw new GameQuitException();`.
De hele aanvalsronde stopt direct en in `Main.java` wordt de noodrem opgevangen:

```java
try {
    pokemonGym.enteredTheGym(player1);
} catch (GameQuitException e) {
    System.out.println("Thank you for playing");
}
```
Resultaat: Geen rode crash-teksten, maar een nette afsluiting!

---

### 2.4 Wat is Typecasting (`LineReaderImpl`)? — *De Vermomming*
Stel: Peter Parker loopt over straat in gewone kleren. Iedereen ziet: *"Dat is een Mens."*
Een Mens kan lopen en praten. Maar een Mens kan niet tegen muren opklimmen.
Pas als Peter zijn Spider-Man pak aantrekt, mag hij webben schieten.

In Java:
```java
private final LineReader reader; // LineReader is de interface (het algemene type)
```
In de interface `LineReader` zit géén methode `setCompleter()`.
Maar we wéten dat de builder achter de schermen een `LineReaderImpl` heeft aangemaakt!
Door te **casten**:
```java
LineReaderImpl readerImpl = (LineReaderImpl) reader;
```
zeggen we tegen de compiler: *"Vertrouw me, dit object is stiekem een `LineReaderImpl`, laat me bij de speciale methodes!"*

---

### 2.5 Run-venster vs Echte Terminal (`wt`) — *Bioscoop vs Storyboard*
Waarom werkte de Tab-toets niet in het standaard "Run" schermpje onderin IntelliJ?
- Het IntelliJ Run-venster is **geen echte terminal (TTY)**. Het is gewoon een tekstvakje waar IntelliJ uitvoer in dumpt.
- Als je in dat tekstvakje op [Tab] drukt, denkt IntelliJ: *"De gebruiker wil naar het volgende knopje springen"* in plaats van dat hij het teken doorgeeft aan Java.
- JLine merkt dit en schakelt over naar **"Dumb Terminal mode"**: hij zet alle tab-functies en pijltjestoetsen uit om te voorkomen dat het vastloopt.

In een **echte terminal** (zoals Windows Terminal `wt`, PowerShell of de *Terminal*-tab onderin IntelliJ) praat JLine direct met het besturingssysteem. Daar werken Tab, pijltjes omhoog/omlaag en kleuren 100%!

---

### 2.6 Het Mysterie van de "Ling-Ling" Bel & de `Supplier` (Lambda)
Tijdens het testen hoorde je opeens *"ling ling"* (de terminal-bel). Wat betekende dat?
In JLine betekent de bel: *"Ik ontving je Tab-toets, maar mijn lijst met keuzes is leeg!"*

**Hoe kwam dat?**
In het begin deden we:
```java
List<String> currentOptions = new ArrayList<>(); // leeg bij start
reader = LineReaderBuilder.builder()
        .completer(new StringsCompleter(currentOptions)) // <-- FOUT!
        .build();
```
`new StringsCompleter(currentOptions)` maakte meteen bij het opstarten een **kopie** van die lege lijst. Toen we later opties toevoegden aan `currentOptions`, wist de completer daar niets van!

**De oplossing: Een `Supplier` (Lambda)**
```java
.completer(new StringsCompleter(() -> currentOptions))
```
Het pijltje `() -> currentOptions` betekent: *"Kopieer de lijst niet nu, maar kijk pas in `currentOptions` op het exacte milliseconde dat de gebruiker op Tab drukt!"*
Hierdoor werkt autocompletion nu dynamisch voor elke vraag!

---

### 2.7 De Onzichtbare Spatie (`.trim()`)
Toen Tab eindelijk werkte, koos je `fireLash`. En toch zei het spel:
`Unknown attack. Please choose from: [fireLash, flameThrower, pyroBall, inferno]`

Waarom?
Wanneer JLine met Tab een woord aanvult, plakt hij er automatisch een spatie achter: `"fireLash "`.
In Java is:
```java
"fireLash ".equalsIgnoreCase("fireLash") // = FALSE! Door die spatie!
```
Door `.trim()` toe te voegen in `JLineIO`:
```java
String line = reader.readLine(prompt + " ");
return line != null ? line.trim() : "";
```
knipt Java alle onzichtbare spaties aan het begin en eind eraf, en klopte de vergelijking meteen!

---

### 2.8 Validatielussen met `while` — *De Portier aan de Deur*
Vroeger:
```java
String answer = speler_A.nextLine();
if (answer.equals("yes")) {
    pokemonGym.enteredTheGym(player1);
}
// Typte je iets anders? Spel stopte direct!
```

Nu staat er een vriendelijke **portier** (`while`-lus):
```java
String answer = io.readLine("Would you like to enter a gym? (yes/no):", List.of("yes", "no"));

while (!answer.equalsIgnoreCase("yes") && !answer.equalsIgnoreCase("no")) {
    io.println("Invalid choice. Please enter 'yes' or 'no'.");
    answer = io.readLine("Would you like to enter a gym? (yes/no):", List.of("yes", "no"));
}
```
Zolang je geen `yes` of `no` antwoordt, laat de portier je niet door en vraagt hij het netjes opnieuw.

---

## 3. Overzicht van alle aangepaste en nieuwe bestanden

| Bestand | Wat is ermee gebeurd? |
|---|---|
| `src/GameIO.java` | **Nieuwe interface**: contract met `println`, `readLine(prompt)` en `readLine(prompt, options)`. |
| `src/GameQuitException.java` | **Nieuwe exception**: erft van `RuntimeException`, wordt gegooid bij Ctrl+C en opgevangen in `Main`. |
| `src/JLineIO.java` | **Nieuwe klasse**: implementeert `GameIO` met JLine 4, dynamic `StringsCompleter`, case-insensitivity en `.trim()`. |
| `src/PokemonGymImpl.java` | `GameIO` geïnjecteerd; alle `Scanner` en `System.out` vervangen; `while`-loops toegevoegd voor `a/f/c`, aanvallen en yes/no. |
| `src/Main.java` | Opent `try (JLineIO io = new JLineIO())`, geeft `io` aan `PokemonGymImpl` en vangt `GameQuitException` op. |
| `pom.xml` | `org.jline:jline:4.4.8` en `exec-maven-plugin:3.5.0` toegevoegd. |
| Git branch | Alles staat veilig op de nieuwe branch **`feature/jline-io`**. |

---

## 4. Hoe speel je de game nu?

Open je favoriete terminal:
- Druk op **Win + R**, typ **`wt`** (Windows Terminal) en druk op Enter.
- Typ:
```powershell
cd "C:\CODING PROJECTS and WEBSITES\GITHUB-NOVI-USER\HUISWERK\5-pokemon-interfaces"
mvn exec:java
```

### Handige sneltoetsen tijdens het spelen:
- **[Tab]**: Vult automatisch aan wat mogelijk is op die plek.
- **Dubbel [Tab]**: Toont een keuzemenuutje als er meerdere opties zijn.
- **[Pijltje omhoog]**: Haalt je vorige invoer terug (history).
- **[Ctrl + C]**: Sluit het spel direct en netjes af.

---

## 5. Brainstorm: Hoe maken we deze game nóg vetter in de terminal?

Hier zijn 7 geweldige ideeën die je in latere stappen kunt toevoegen om er een échte arcade-ervaring van te maken:

### 1. Visuele HP-balken met ASCII en Kleur 🟩🟨🟥
In plaats van alleen: `Raichu loses 33 hp. Remaining: 127`
Kunnen we een echte retro Pokémon health bar renderen:
```text
Raichu    [████████████░░░░] 127/160 HP
Venusaur  [██████░░░░░░░░░░]  45/135 HP
```
- Boven de 50% HP: Groen gekleurd
- Tussen 20% en 50%: Geel/Oranje gekleurd
- Onder de 20%: Rood knipperend!

### 2. Retro ASCII-Art van de Pokémon 👾
Als een Pokémon het veld op komt, tonen we een cool tekst-plaatje in de console:
```text
      \`.----\`/
       /  _  _ \   Pika Pika!
      (  (o)(o) )  Raichu betreedt de arena!
      (,  (_)  ,)
```

### 3. Typewriter Effect (Tekst die letter voor letter verschijnt) 📜
In echte Pokémon games verschijnt tekst niet in één klap, maar loopt het rustig over het scherm:
```java
public void typewriter(String text) {
    for (char c : text.toCharArray()) {
        System.out.print(c);
        Thread.sleep(25); // 25 milliseconden pauze per letter
    }
    System.out.println();
}
```
Dit geeft dialogen met Brock direct een dramatisch RPG-gevoel!

### 4. Gekleurde Gevechtsberichten met JLine `AttributedString` 🎨
- Vuur-aanvallen in fel **rood/oranje** (`🔥 Inferno!`)
- Water-aanvallen in **blauw** (`💧 Surf!`)
- Gras-aanvallen in **groen** (`🍃 Leaf Storm!`)
- Bliksem in **felgeel** (`⚡ Electro Ball!`)
- Kritieke treffers en HP-verlies in opvallende kleuren.

### 5. Scherm wissen tussen de beurten (`cls` / clear screen) 🧹
Nu scrollt de terminal eindeloos naar beneden. Met `terminal.puts(InfoCmp.Capability.clear_screen)` of ANSI escape `\033[H\033[2J` kunnen we het scherm leegmaken bij elke nieuwe gevechtsronde. Dan lijkt het een echt vast gamescherm!

### 6. Geluidjes met de Terminal Bell 🔔
Bij een super-effectieve aanval of wanneer je wint, kun je de terminal-bel laten rinkelen (`terminal.writer().write("\007")`).

### 7. Save Game & Highscore (JSON of tekstbestand) 💾
Na afloop van het spel opslaan hoeveel badges je hebt gehaald in een bestandje `savegame.json`. De volgende keer dat je opstart, herkent Brock je: *"Welkom terug Jer, je hebt al 2 badges!"*

---
*Gefeliciteerd met deze grote stap in Java! Je code is nu niet alleen modulair en object-georiënteerd, maar gebruikt ook moderne terminal-technologie.*
