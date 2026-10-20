# 🎵 Complete Gids: 8-Bit Audio Synthese, Jackson JSON, JLine Mouse Support & 6v6 Game-Logica

Deze gids is het vervolg op [`JLINE_IO_EN_TERMINAL_GIDS.md`](./JLINE_IO_EN_TERMINAL_GIDS.md) en legt stap voor stap **alle nieuwe technieken** uit die we aan de Pokémon Gym Arcade Edition hebben toegevoegd. Alles is zo geschreven dat je precies begrijpt *waarom* de code zo werkt en hoe je deze patronen in toekomstige Java-projecten kunt hergebruiken.

---

## Inhoudsopgave
1. [Project- en Package-architectuur](#1-project--en-package-architectuur)
2. [Hoe werkt 8-Bit Geluid in Java? (1-Kanaals vs. Multi-Kanaals)](#2-hoe-werkt-8-bit-geluid-in-java-1-kanaals-vs-multi-kanaals)
3. [Waarom lagde de oude 1-kanaals loop én waarom gaf `Clip` een Maven-thread warning?](#3-waarom-lagde-de-oude-1-kanaals-loop-én-waarom-gaf-clip-een-maven-thread-warning)
4. [JSON Parsen: Handmatig (`ManualJsonParser`) vs. Jackson (`ChiptuneJsonSynth`)](#4-json-parsen-handmatig-manualjsonparser-vs-jackson-chiptunejsonsynth)
5. [Live Volume (10%–100%) & Tempo Slider (0.50x–2.00x) zonder Chipmunk-effect](#5-live-volume-10100--tempo-slider-050x200x-zonder-chipmunk-effect)
6. [Het Pop-up Settings Menu (`[s]`): Alternate Screen Buffer, Pijltjes & Muis-coördinaten](#6-het-pop-up-settings-menu-s-alternate-screen-buffer-pijltjes--muis-coördinaten)
7. [`ChoiceHelper`: Snel kiezen met `1..9`, `a..z` en `y/n`](#7-choicehelper-snel-kiezen-met-19-az-en-yn)
8. [Visuele Terminal-effecten: Pokéball-animatie (`\033[7A`), Regelafstand & Uitlijning](#8-visuele-terminal-effecten-pokéball-animatie-0337a-regelafstand--uitlijning)
9. [De echte 6-tegen-6 Pokémon Gym Battle Flow](#9-de-echte-6-tegen-6-pokémon-gym-battle-flow)
10. [Bug-Postmortem & Leerpunten: 5 échte Bugs die we tegenkwamen en hoe je ze oplost](#10-bug-postmortem--leerpunten-5-échte-bugs-die-we-tegenkwamen-en-hoe-je-ze-oplost)

---

## 1. Project- en Package-architectuur

Om het project overzichtelijk te houden (Separation of Concerns), hebben we alle klassen gegroepeerd in logische Java-packages:

```text
src/
 ├── Main.java                       # Startpunt: menu-keuzes en opstarten van de Gym
 ├── Pokemon/                        # Domeinmodel: Pokemon, subklassen en aanvalsberekening
 │    ├── Pokemon.java
 │    ├── PokemonType.java
 │    ├── AttackTypeHelper.java
 │    ├── AttackResult.java
 │    └── Fire/Water/Grass/ElectricPokemon.java
 ├── gym/                            # Spelregels & 6v6 Gevechts-engine
 │    ├── PokemonGym.java
 │    ├── PokemonGymImpl.java
 │    ├── PokemonTrainer.java
 │    └── PokemonGymOwner.java
 ├── io/                             # Terminal Invoer/Uitvoer, Settings-loop & Keuze-helper
 │    ├── GameIO.java
 │    ├── JLineIO.java
 │    ├── ChoiceHelper.java
 │    └── GameQuitException.java
 ├── ui/                             # Visuele weergave: HUD, Settings Dashboard, ASCII-art & Divoom sync
 │    └── ArcadeUI.java
 ├── audio/                          # 8-Bit Geluids-engine, Jackson JSON Synth & Bewaarde Parser
 │    ├── SoundEngine.java
 │    ├── ChiptuneJsonSynth.java
 │    └── ManualJsonParser.java
 └── test/                           # Losse test-scenario's
```

---

## 2. Hoe werkt 8-Bit Geluid in Java? (1-Kanaals vs. Multi-Kanaals)

Geluid uit je koptelefoon of speakers is niets anders dan een trilling van lucht. In Java sturen we een array van getallen (`byte[]`) naar de geluidskaart. Die getallen geven duizenden keren per seconde (de **Sample Rate**, bijv. `16.000` of `22.050` keer per seconde) aan hoe ver de speaker-conus naar voren (`+`) of naar achteren (`-`) moet staan.

### A. De 8-Bit Blokgolf (Square Wave)
Een echte Nintendo GameBoy maakt geen vloeiende viool-golven, maar een **blokgolf** (*square wave*): de speaker springt telkens direct tussen `+volume` en `-volume`:

```text
+16 |  ██████      ██████      ██████
  0 |──      ──────      ──────      ──
-16 |        ██████      ██████
       |<--->|
       1 periode = SAMPLE_RATE / frequentie (Hz)
```

In [`SoundEngine.java`](../src/audio/SoundEngine.java) berekenen we dat als volgt:
```java
int period = Math.max(2, (int) (SAMPLE_RATE / hz));
for (int i = 0; i < noteSamples; i++) {
    // Eerste helft van de periode: +amp, tweede helft: -amp
    buf[cursor++] = (i % period < period / 2) ? amp : (byte) -amp;
}
```
- **Hogere `hz` (bijv. 880 Hz)** $\rightarrow$ kortere periode $\rightarrow$ de speaker wisselt sneller $\rightarrow$ **hogere toon**.
- **Lagere `hz` (bijv. 220 Hz)** $\rightarrow$ langere periode $\rightarrow$ de speaker wisselt langzamer $\rightarrow$ **lagere bastoon**.

### B. Van 1-Kanaals naar Multi-Kanaals (Mixen!)
- **1-Kanaals (`SINGLE`)**: Speelt één frequentie tegelijk af uit een 2D-array `int[][] { {hz, duurMs}, ... }`.
- **Multi-Kanaals (`MULTI_1`, `MULTI_2`, `MULTI_3`)**: In een JSON-track uit BeepBox/JummBox spelen **4 tot 20 kanalen tegelijk** (hoofdmelodie, harmonie, baslijn en drums).
  Hoe laat je één speaker meerdere noten tegelijk afspelen? **Door de geluidsgolven bij elkaar op te tellen (`mix[s] += ...`)!**
  - Als Kanaal 1 op tijdstip $s$ waarde `+0.25` heeft en Kanaal 2 heeft waarde `+0.15`, dan krijgt `mix[s]` de som `+0.40`.
  - Aan het einde van [`ChiptuneJsonSynth.synthesizeSongPcm()`](../src/audio/ChiptuneJsonSynth.java) **normaliseren** we de hele `float[] mix` array zodat de hoogste piek altijd precies op `0.85` (85% volume) zit. Daardoor overstuurt (clipt) het geluid nooit, zelfs niet bij een track met 20 kanalen!

### C. MIDI-noten omrekenen naar Hertz ($f$) en GameBoy Drums (LFSR)
In de JSON-bestanden staan geen Hertz-getallen, maar **MIDI-nootnummers** (waarbij `69` = de noot **A4** van `440 Hz`, en elk heel getal hoger of lager één halve toon op een piano is).
Elke 12 halve tonen (1 octaaf) verdubbelt de frequentie. Daarom rekenen we een MIDI-noot $p$ om naar Hertz met:
$$f = 440 \times 2^{\frac{p - 69}{12}}$$
In Java:
```java
double freqHz = 440.0 * Math.pow(2.0, (midiPitch - 69.0) / 12.0);
```
Voor het **drum-kanaal** (`"type": "drum"`) gebruiken we een **15-bit LFSR (*Linear Feedback Shift Register*)**: een bit-schuifregister (`(lfsr ^ (lfsr >> 1)) & 1`) dat exact dezelfde retro digitale witte ruis opwekt als de originele GameBoy-geluidschip voor snare-drums en hi-hats.

---

## 3. Waarom lagde de oude 1-kanaals loop én waarom gaf `Clip` een Maven-thread warning?

Hier hebben we twee hele interessante Java Sound-lessen geleerd:

### Probleem 1: Per losse noot een `SourceDataLine` openen en sluiten (200ms lag per noot!)
Toen we eerst een `while`-loop hadden die voor elke losse noot een nieuwe `SourceDataLine` opende, gebeurde dit bij **elke noot**:
```java
// OUDE SITUATIE (NIET DOEN VOOR MUZIEK!):
try (SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
    line.open(af, buf.length); // <-- ~100ms Windows driver-open tijd!
    line.start();
    line.write(buf, 0, buf.length);
    line.drain();              // <-- Wacht in C++ driver en blokkeert Thread.interrupt()!
}                              // <-- ~80ms Windows driver-close tijd!
```
Op Windows kost het openen, legen (`drain`) en sluiten van een hardware-audiolijn voor één enkele noot bijna **200 milliseconden extra**. Een snelle noot van `70 ms` duurde daardoor `270 ms` (4x te langzaam!).

### Probleem 2: Waarom `AudioSystem.getClip()` bij het afsluiten 15 seconden bleef hangen
Toen we overstapten op `javax.sound.sampled.Clip`, speelde de muziek strak af, maar bij het afsluiten van het spel (`q`) kreeg je deze Maven-waarschuwing en duurde afsluiten 15 seconden:
```text
[WARNING] thread Thread[#79,Java Sound Event Dispatcher,5,Main] was interrupted but is still alive after waiting at least 14921msecs
[WARNING] thread Thread[#79,Java Sound Event Dispatcher,5,Main] will linger despite being asked to die via interruption
```
**Waarom gebeurt dit?**
Zodra je in Java `AudioSystem.getClip()` aanroept, start de JDK onder de motorkap een interne achtergrond-thread genaamd **`Java Sound Event Dispatcher`**. Door een bekende eigenschap in de JDK is dat **géén daemon-thread**, en die thread negeert `Thread.interrupt()` en blijft na `clip.close()` nog ~15 seconden wachten voordat hij stopt!

### De Ultieme Oplossing: Een eigen Daemon PCM-Streamer met Chunks van 40ms!
In [`SoundEngine.startBgmStreamer()`](../src/audio/SoundEngine.java) gebruiken we nu **géén `Clip` meer**, maar onze eigen **Daemon Thread (`Pokemon-BGM-Streamer`)** die de volledige `byte[] pcm` array in kleine blokjes (**chunks van 40 milliseconden**, oftewel ~880 bytes) naar één openstaande `SourceDataLine` schrijft:
1. **`bgmThread.setDaemon(true)`**: Er wordt géén `Java Sound Event Dispatcher` thread meer aangemaakt. Zodra je op `q` drukt, sluit het spel binnen **0 milliseconden** af zonder één enkele Maven-warning!
2. **Geen 200ms driver-lag**: Omdat we de `SourceDataLine` maar **één keer** openen voor het hele liedje en kleine blokjes van 40ms achter elkaar in de buffer schrijven, speelt de 1-kanaals track 100% strak op tempo.
3. **Live Volume & Tempo binnen 40ms**: Omdat er elke 40ms een nieuw blokje naar de geluidskaart gaat, worden wijzigingen aan de Volume-slider of Tempo-slider **direct tijdens het volgende 40ms-blokje** hoorbaar!

---

## 4. JSON Parsen: Handmatig (`ManualJsonParser`) vs. Jackson (`ChiptuneJsonSynth`)

### A. De moderne manier met Jackson (`ChiptuneJsonSynth.java`)
In [`pom.xml`](../pom.xml) hebben we `com.fasterxml.jackson.core:jackson-databind` toegevoegd.
Met Jackson hoef je alleen Java `record`s te maken waarvan de veldnamen overeenkomen met de sleutels in het JSON-bestand:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChiptuneSong(
        String name,
        int beatsPerMinute,
        int beatsPerBar,
        int ticksPerBeat,
        List<Channel> channels
) {}
```
- **`@JsonIgnoreProperties(ignoreUnknown = true)`**: Dit vertelt Jackson: *"Als er in het JSON-bestand nog 30 andere instellingen staan die ik niet nodig heb (zoals `reverb` of `eqFilter`), negeer die dan gerust zonder een foutmelding te geven."*
- Vervolgens leest één regel code een bestand van 3.500 regels in naar kant-en-klare Java-objecten:
  ```java
  ChiptuneSong song = MAPPER.readValue(jsonPath.toFile(), ChiptuneSong.class);
  ```

### B. Hoe werkte de handmatige parser (`ManualJsonParser.java`)?
In [`ManualJsonParser.java`](../src/audio/ManualJsonParser.java) hebben we de handgebouwde parser bewaard zodat je kunt zien hoe een parser onder de motorkap werkt:
- **Bracket Depth Counting (`findMatchingBracket` en `splitTopLevelObjects`)**:
  JSON bestaat uit geneste blokken `{ ... }` en lijsten `[ ... ]`. Als je wilt weten waar een lijst `[` eindigt, kun je niet simpelweg zoeken naar de eerstvolgende `]`, want binnenin zitten óók kleinere lijsten!
  Daarom telt de handmatige parser de **diepte (`depth`)**:
  - Bij elke `[` die hij tegenkomt: `depth++`
  - Bij elke `]` die hij tegenkomt: `depth--`
  - Pas wanneer `depth == 0` is, heb je het bijbehorende sluithaakje gevonden!

---

## 5. Live Volume (10%–100%) & Tempo Slider (0.50x–2.00x) zonder Chipmunk-effect

### A. Live Volume schalen per 40ms Chunk
In [`SoundEngine.startBgmStreamer()`](../src/audio/SoundEngine.java) vermenigvuldigen we elke sample uit de audiobuffer vlak voordat hij naar de speaker gaat met `volumePercent / 100.0`:
```java
double volScale = volumePercent / 100.0;
for (int i = 0; i < len; i++) {
    int scaled = (int) Math.round(pcm[cursor + i] * volScale);
    scaledChunk[i] = (byte) Math.max(-127, Math.min(127, scaled));
}
```
Hierdoor kun je het volume live tussen **10% en 100%** schuiven zonder dat het liedje opnieuw gerenderd hoeft te worden!

### B. Tempo (0.50x – 2.00x) aanpassen zonder Chipmunk-effect
Als je bij digitale audio simpelweg de `SAMPLE_RATE` verdubbelt om muziek 2x zo snel af te spelen, verdubbelen ook alle frequenties in Hertz en klinkt alles een octaaf hoger (het *Chipmunk-effect*).

Om het **tempo (BPM)** wél aan te passen terwijl de **toonhoogte (pitch)** 100% zuiver blijft:
1. We houden de `SAMPLE_RATE` (`22050 Hz`) en de frequentie-formule ($440 \times 2^{(p-69)/12}$) exact gelijk.
2. We vermenigvuldigen alléén de `beatsPerMinute` met de `tempoMultiplier` (`0.50` t/m `2.00`):
   ```java
   double effectiveBpm = baseBpm * tempoMultiplier;
   double tickSec = (60.0 / effectiveBpm) / ticksPerBeat;
   ```
3. **Naadloos verder spelen op dezelfde plek (`currentProgressRatio`)**:
   Onze daemon-streamer houdt bij elk 40ms-blokje bij waar hij procentueel in het nummer zit (`currentProgressRatio = cursor / (double) pcm.length`). Verander je het tempo in het Settings-scherm, dan start de nieuwe PCM-buffer direct op exact datzelfde percentage!

---

## 6. Het Pop-up Settings Menu (`[s]`): Alternate Screen Buffer, Pijltjes & Muis-coördinaten

Net zoals in een echte game zit alles rondom **Audio & Layout** onder één knop: typ op elk gewenst moment **`s`** (of `settings`) en je opent een **Pop-up Settings Venster** ([`ArcadeUI.renderSettingsDashboard()`](../src/ui/ArcadeUI.java) + [`JLineIO.openSettingsScreen()`](../src/io/JLineIO.java)).

### A. Hoe open en sluit je een venster zónder gevechts-logs kwijt te raken?
In [`ArcadeUI.enterSettingsOverlay()`](../src/ui/ArcadeUI.java) en [`ArcadeUI.exitSettingsOverlay()`](../src/ui/ArcadeUI.java) gebruiken we de **ANSI Alternate Screen Buffer (`\033[?1049h` en `\033[?1049l`)**:
1. **Bij openen (`s`)**:
   - Eerst wissen we de regel waar je net `Choice: s` hebt getypt (`\033[1A\033[2K\r`).
   - Daarna sturen we `\033[?1049h\033[H\033[2J\033[?25l`:
     - `\033[?1049h`: Slaat je **volledige gevechtsscherm** op en schakelt over naar een aparte pop-up pagina (precies zoals `vim` of `htop`).
     - `\033[H\033[2J`: Zet de cursor op de nieuwe pagina gegarandeerd linksboven op **`(x=0, y=0)`** en maakt de pop-up pagina schoon.
     - `\033[?25l`: Verbergt de knipperende tekstcursor zolang je in het menu zit.
2. **Tijdens het aanpassen in het menu**:
   - Bij elke pijltjestoets of muisklik sturen we alléén `\033[H` (zet cursor terug naar `(0, 0)` zonder scherm-wis). Daardoor overschrijft het venster zichzelf **zonder enig geknipper (flicker-free)**!
3. **Bij sluiten (`Enter`, `Esc`, `0`, of klik op `[✖ SLUITEN: 0]`)**:
   - We sturen `\033[H\033[2J\033[?25h\033[?1049l`. De pop-up pagina sluit en je bent **op de millimeter terug in je gevecht** met **100% van al je eerdere gevechts-logs** nog op het scherm!

### B. Hoe werken de Muis-coördinaten `(x, y)` op het Settings-rooster?
Doordat het Settings-venster op de Alternate Screen Buffer altijd op `(x=0, y=0)` begint, ligt elke rij en elke slider op een **vaste rij (`y`) en kolom (`x`)**:

```text
y=0  : ╔═════════════════════════════════════════════════════════════════════╗
y=1  : ║ ⚙️  POKÉMON GYM — SETTINGS MENU                      [✖ SLUITEN: 0] ║  <-- x=50..68 = Sluit-knop!
y=2  : ╠═════════════════════════════════════════════════════════════════════╣
y=3  : ║  1) 🔊 Geluid Aan/Uit   : ON (Actief)  [Klik of ←/→ om te Muten]    ║  <-- clickedRow = y - 2 = 1
y=4  : ║  2) 🎵 Soundtrack (1-5) : [1/5] Multi-Channel 1: Pokémon Red/Blue   ║  <-- clickedRow = y - 2 = 2
y=5  : ║ ►3) 🎚️ Muziek Volume    : [-] [████████████░░░░░] [+]  70%          ║  <-- clickedRow = y - 2 = 3
y=6  : ║  4) ⏱️ Muziek Tempo     : [-] [─────●───────────] [+] 1.00x (100%)  ║  <-- clickedRow = y - 2 = 4
y=7  : ║  5) 🎨 ASCII Art Stijl  : Color Pixel-Art (Divoom)                  ║  <-- clickedRow = y - 2 = 5
y=8  : ║  6) 📏 Regelafstand     : Normaal (1 witregel tussen secties)       ║  <-- clickedRow = y - 2 = 6
y=9  : ║  7) ⌨️ Tekstsnelheid    : Normaal (12 ms per letter)                ║  <-- clickedRow = y - 2 = 7
y=10 : ║  8) 🔄 Reset Standaard  : [Klik op 8) of druk 8 om te resetten]     ║  <-- clickedRow = y - 2 = 8
       |                         |  |   |               |   |
     x=0                       x=27 29  33             49   53
                                [-]     [  17 streepjes ]   [+]
```
- **In willekeurige volgorde op `1)` t/m `8)` klikken**: Uit de geklikte rij `y` berekenen we direct `int clickedRow = y - 2;`. Klik je op `y = 7`, dan weet Java direct dat je op **`5) 🎨 ASCII Art Stijl`** hebt geklikt!
- **Op de Volume- of Tempo-slider klikken (`clickedRow == 3` of `4`)**:
  - `x` tussen `27` en `31`: Klik op `[-]` $\rightarrow$ verlaag met 10%.
  - `x` tussen `32` en `50`: Klik op de 17 streepjes van de slider-balk $\rightarrow$ bereken de verhouding `(x - 33.0) / 16.0` en spring direct naar dat exacte Volume of Tempo!
  - `x` tussen `51` en `56`: Klik op `[+]` $\rightarrow$ verhoog met 10%.

---

## 7. `ChoiceHelper`: Snel kiezen met `1..9`, `a..z` en `y/n`

In [`ChoiceHelper.java`](../src/io/ChoiceHelper.java) zorgen we dat de speler nooit lange woorden hoeft over te typen:
- **Index `0` t/m `8`** krijgen automatisch sleutel `"1"` t/m `"9"`.
- **Vanaf index `9` (het 10e item)** tellen we automatisch door met het alfabet (`"a"`, `"b"`, `"c"`, ...) via karakter-rekenen in Java:
  ```java
  char letter = (char) ('a' + (index - 9));
  ```
  Omdat een `char` onder water een Unicode/ASCII-getal is (`'a'` = 97), levert `'a' + 0` de letter `'a'`, `'a' + 1` de letter `'b'`, enzovoort!
- Doordat [`JLineIO.readLine()`](../src/io/JLineIO.java) aan het einde altijd `ChoiceHelper.resolveInput(trimmed, options)` aanroept, krijgt de rest van je spel altijd netjes de volledige optienaam terug (bijv. `"Charrizard"` of `"yes"`), of de speler nu `1`, `1)`, `y` of `Charrizard` heeft getypt!

---

## 8. Visuele Terminal-effecten: Pokéball-animatie (`\033[7A`), Regelafstand & Uitlijning

1. **In-Place Animatie (`\033[7A`)**:
   Normaal scrollt een terminal altijd naar beneden als je iets print. Om de Pokéball op **exact dezelfde plek** heen en weer te laten schudden, printen we een frame van 7 regels hoog, wachten `360 ms`, en sturen dan de ANSI-code `\033[7A` (*Move Cursor Up 7 Lines*). Het volgende frame overschrijft daardoor precies het vorige frame!
2. **Geen dubbele HUD meer + Instelbare Regelafstand (`printSectionSpacing`)**:
   De Battle HUD wordt nu nog maar **1x per ronde** getekend (vlak voordat jij aan de beurt bent, zodat je direct de actuele HP na Brock's aanval ziet). Met de instelling **Regelafstand** (`Compact` / `Normaal` / `Ruim`) in het Settings-scherm bepaal je zelf hoeveel witregels [`ArcadeUI.printSectionSpacing()`](../src/ui/ArcadeUI.java) tussen de beurten plaatst.
3. **Automatische Rechter-uitlijning naast ASCII-art (`visualLength`)**:
   Sommige ASCII-bestanden gebruiken gewone tekens (1 kolom breed), terwijl de gekleurde Divoom/Pixel-art bestanden vierkante emoji-blokjes (`⬜`, `🟥`, `⬛`) gebruiken die in de terminal **2 kolommen breed** zijn.
   [`ArcadeUI.visualLength()`](../src/ui/ArcadeUI.java) telt voor elk emoji-blokje `2` kolommen en voor gewone tekens `1` kolom, zodat de naam en HP rechts naast de Pokémon altijd kaarsrecht onder elkaar staan!

---

## 9. De echte 6-tegen-6 Pokémon Gym Battle Flow

In [`PokemonGymImpl.java`](../src/gym/PokemonGymImpl.java) werkt het gevecht nu precies zoals in de originele GameBoy Pokémon-games:
1. **Wisselen kost je beurt**: Kies je `c` (Change Pokémon), dan roep je een nieuwe Pokémon op en is daarna direct Brock aan de beurt om aan te vallen.
2. **Doorlopend 6-tegen-6 gevecht**:
   - De `while`-loop draait zolang beide trainers nog minstens één levende Pokémon hebben (`!allFainted(trainer.getPokemons()) && !allFainted(owner.getPokemons())`).
   - Valt jouw Pokémon flauw (`0 HP`), dan kies je direct je volgende levende Pokémon en vecht je verder tegen dezelfde (al verzwakte!) Pokémon van Brock.
   - Valt Brock's Pokémon flauw (`0 HP`), dan gooit Brock direct zijn volgende Pokéball de arena in, totdat alle 6 zijn verslagen en je de **Pewter Gym Badge** 🏆 wint!

---

## 10. Bug-Postmortem & Leerpunten: 5 échte Bugs die we tegenkwamen en hoe je ze oplost

Tijdens het bouwen van deze features kwamen we 5 klassieke problemen tegen die elke ervaren Java/CLI-developer vroeg of laat meemaakt. Hier is het overzicht van **wat er misging**, **waarom het gebeurde** en **hoe je het oplost**:

| # | Symptoom / Bug | Oorzaak onder de motorkap | Onze Oplossing |
| :--- | :--- | :--- | :--- |
| **1** | **1-Kanaals track speelde 4x te traag en liep achter bij snel wisselen** | In een `while`-loop werd voor *elke losse noot* `AudioSystem.getSourceDataLine()` geopend, geleegd (`drain()`) en gesloten (~200ms Windows driver-overhead per noot, plus `drain()` negeert `Thread.interrupt()`). | Alle noten vooraf in één `byte[]` PCM-array achter elkaar zetten en via één open `SourceDataLine` in blokjes van **40ms** streamen. |
| **2** | **15 seconden vertraging bij afsluiten (`q`) + Maven warning `Java Sound Event Dispatcher will linger`** | `AudioSystem.getClip()` start intern in de JDK een **niet-daemon thread** (`Java Sound Event Dispatcher`) die na `clip.close()` nog 15 seconden blijft wachten en de JVM wakker houdt. | Géén `Clip` meer gebruiken, maar een eigen `Thread` met **`setDaemon(true)`** (`Pokemon-BGM-Streamer`) die 40ms PCM-chunks schrijft. De JVM sluit nu in **0ms** af! |
| **3** | **Muis-slider in de Battle HUD spamde `🖱️ [Mouse Slider]` regels door het gevecht heen** | Tijdens het gevecht scrollt de terminal continu naar beneden én stond `MouseTracking.Normal` altijd aan. Elke muisklik in het venster riep `reader.printAbove(...)` aan. | Tijdens het gevecht staat `MouseTracking.Off` **uit**. Alle sliders zitten in een apart **Pop-up Settings Venster (`s`)** waar `MouseTracking.Normal` wél aanstaat. |
| **4** | **`Ctrl + Scrollwiel` veranderde in Settings het Tempo i.p.v. Font Zoom, en Scrollwiel werkte niet op Volume** | Zodra een terminal in `MouseTracking.Normal` staat, stuurt hij `Wheel`-events naar de app. Onze handler koppelde elk `Wheel`-event vast aan `adjustTempo()`. | Het Scrollwiel kijkt nu naar **waar je muis zweeft (`hoveredRow = y - 2`)** én naar **welke rij geselecteerd is (`►`)**: zweef/selecteer je rij 3, dan scrollt hij **Volume**; bij rij 4 **Tempo**! |
| **5** | **Klikken met de muis op `1)`..`8)` in het Settings-pop-up venster reageerde eerst op de verkeerde rij** | `\033[?1049h` schakelt wel naar de Alternate Screen Buffer, maar laat in Windows Terminal de cursor op de *oude `Y`-rij onderaan het scherm* staan! Daardoor begon het menu op bijv. `y = 24` i.p.v. `y = 0`. | Direct na `\033[?1049h` óók **`\033[H\033[2J`** sturen in `enterSettingsOverlay()`. Nu staat de linkerbovenhoek van het menu gegarandeerd op `(0, 0)` en is rij `1)..8)` altijd exact `y - 2`! |

