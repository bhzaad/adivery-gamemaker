// Poll events (optional)
var _ev = adivery_get_last_event();
if (_ev != "") {
    show_debug_message("Adivery: " + string(_ev));
}

// Auto-show an interstitial when it's ready (example)
if (!shown_interstitial) {
    if (adivery_is_loaded(global.adivery_placement_interstitial) == 1) {
        adivery_show(global.adivery_placement_interstitial);
        shown_interstitial = true;
    }
}

