# PawPlace · Android logo assets

Pin-Paw mark, ready to drop into a Kotlin/Android Studio project.

## File layout

Copy this folder into your module's `src/main/`:

```
app/src/main/
├── AndroidManifest.xml          ← reference @mipmap/ic_launcher (already standard)
└── res/
    ├── drawable/
    │   ├── ic_launcher_background.xml   ← deep-space radial (108dp)
    │   ├── ic_launcher_foreground.xml   ← Pin-Paw mark (108dp, 66dp safe zone)
    │   ├── ic_launcher_monochrome.xml   ← Android 13+ themed icon
    │   └── ic_pawplace_logo.xml         ← in-app logo (splash, headers)
    ├── mipmap-anydpi-v26/
    │   ├── ic_launcher.xml              ← adaptive icon (square)
    │   └── ic_launcher_round.xml        ← adaptive icon (round)
    └── values/
        └── colors.xml                   ← brand color tokens
```

> **Heads-up:** for legacy Android (<8.0), Android Studio's *Image Asset Studio*
> will generate the `mipmap-mdpi/…/xxxhdpi` PNG fallbacks for you — point it at
> `ic_launcher_foreground.xml` + `ic_launcher_background.xml`.

## AndroidManifest

The manifest entry is the standard one — no change needed if you scaffolded
with Android Studio's default template:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ... >
```

## Using the logo inside the app

### Jetpack Compose
```kotlin
Image(
    painter = painterResource(R.drawable.ic_pawplace_logo),
    contentDescription = "PawPlace",
    modifier = Modifier.size(96.dp)
)
```

### View system
```xml
<ImageView
    android:layout_width="96dp"
    android:layout_height="96dp"
    android:src="@drawable/ic_pawplace_logo"
    android:contentDescription="PawPlace" />
```

### Splash screen (Android 12+ SplashScreen API)
```xml
<!-- res/values/themes.xml -->
<style name="Theme.PawPlace.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/pp_bg</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.PawPlace</item>
</style>
```

## Brand colors (already in `colors.xml`)

| Token | Hex | Use |
|---|---|---|
| `pp_accent` | `#7DD8FF` | primary cyan |
| `pp_accent_deep` | `#1E92BF` | accent on light |
| `pp_ink` | `#0A1218` | primary text on light |
| `pp_bg` | `#070D11` | dark background |
| `pp_on_dark` | `#F2F6F8` | text on dark |

## Notes

- The mark uses `evenOdd` fill so the paw cuts cleanly into the pin.
- All paths fit within the 66dp safe zone — won't clip on any launcher mask.
- Monochrome variant supports Android 13+ themed icons (Material You).
