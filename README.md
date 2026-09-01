# Adivery GameMaker Extension

An unofficial, native extension for **GameMaker** that integrates the **Adivery** ad network SDK into Android mobile games. This extension allows game developers to easily monetize their GameMaker projects using Adivery's ad placements.

---

## Features
- **Lightweight Integration:** Quick initialization using your Adivery App ID.
- **Rewarded Video Ads:** Reward players instantly by tracking completed video views.
- **Interstitial Ads:** Trigger full-screen ads at natural break points in your gameplay.
- **Asynchronous Callbacks:** Built-in support for GameMaker's Social Async Event to detect when ads load, open, or close.

---

## Directory Structure
The repository is structured as a standard GameMaker Project (`.yyp`):
- `extensions/Adivery`: Contains the native Android binaries and GML mapping definitions.
- `objects/obj_adivery`: A helper controller object demonstrating implementation.

---

## Installation & Setup

1. **Clone or Download** this repository.
2. Open your existing project in GameMaker, right-click on **Extensions**, select **Import Extension**, and choose the `Adivery` extension folder from this project.
3. Drag and drop `obj_adivery` into your game's initialization or splash room.

---

## Code Examples

### 1. Initialization
Place this in the **Create Event** of your persistent ad manager object:
```gml
// Initialize the Adivery SDK with your unique App ID
adivery_init("YOUR_APP_ID_HERE");
```

### 2. Loading and Showing Rewarded Ads
```gml
// Load a rewarded ad using your Placement ID
adivery_rewarded_load("YOUR_PLACEMENT_ID");

// Check readiness and trigger display
if (adivery_rewarded_is_loaded("YOUR_PLACEMENT_ID")) {
    adivery_rewarded_show("YOUR_PLACEMENT_ID");
}
```

### 3. Handling Rewards (Social Async Event)
Add a **Social Async Event** to your controller object to capture callbacks dispatched from the native SDK:

```gml
var _type = ds_map_find_value(async_load, "type");

if (_type == "adivery_rewarded_callback") {
    var _event = ds_map_find_value(async_load, "event");
    
    // Reward the player when the ad is completely watched
    if (_event == "onRewarded") {
        global.coins += 100; 
    }
}
```

---

## API Reference

| Function | Description |
| :--- | :--- |
| `adivery_init(app_id)` | Initializes the native network core. |
| `adivery_interstitial_load(placement_id)` | Caches an interstitial ad in the background. |
| `adivery_interstitial_is_loaded(placement_id)` | Returns `true` if the interstitial ad is ready to show. |
| `adivery_interstitial_show(placement_id)` | Displays the cached interstitial ad overlay. |
| `adivery_rewarded_load(placement_id)` | Caches a rewarded video ad. |
| `adivery_rewarded_is_loaded(placement_id)` | Returns `true` if the rewarded video is ready. |
| `adivery_rewarded_show(placement_id)` | Plays the rewarded video asset. |

---

## Contributing
Contributions are highly appreciated! If you want to expand support for iOS (`.mm` wrapper), implement Banner Ads, or patch existing bugs:
1. Fork this repository.
2. Create a feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a **Pull Request**.

---

## License
This project is licensed under the MIT License - see the LICENSE file for details.
