# DAC Awake

Egyszerű Android alkalmazás USB DAC-ok (pl. AB13X USB Audio) ébren tartására.

## Mit csinál?
- 48 kHz / 16 bit / sztereó AudioTrack streamet tart nyitva.
- 997 Hz-es, rendkívül alacsony szintű jelet generál.
- Nem kér audio fókuszt, ezért más appok hangja (pl. Duolingo) rákeverhető.
- Foreground service-ként fut, így a háttérben is stabilabb.
- A jelszint 1..128 PCM között állítható. Kezdőérték: 4 (~-78 dBFS).

## Építés
Android Studio -> Open -> válaszd ki a DACAwake mappát -> Build APK.

A projekt compileSdk/targetSdk 35-re van állítva. Ha az Android Studio újabb SDK-t ajánl, nyugodtan frissíthető.

## Használat
1. USB-C fülhallgató/DAC csatlakoztatása.
2. DAC Awake megnyitása.
3. INDÍTÁS.
4. Duolingo teszt.
5. Ha még levágja a szó elejét, a csúszkát lassan emeld.
6. A legkisebb működő értéket használd.

## HyperOS
Ha a rendszer leállítja a háttérben:
Beállítások -> Alkalmazások -> DAC Awake -> Akkumulátor -> Korlátozás nélkül.
