// Handle async events from AdiveryGM (DS map-based)
var _map = async_load;
if (!is_undefined(_map)) {
    var _ev = ds_map_find_value(_map, "adivery_event");
    if (!is_undefined(_ev)) {
        var _placement = "";
        var _message = "";
        var _rewarded = -1;
        if (ds_map_exists(_map, "placement")) _placement = string(ds_map_find_value(_map, "placement"));
        if (ds_map_exists(_map, "message")) _message = string(ds_map_find_value(_map, "message"));
        if (ds_map_exists(_map, "rewarded")) _rewarded = ds_map_find_value(_map, "rewarded");

        global.adivery_last_event = "type=" + string(_ev)
            + ";placement=" + string(_placement)
            + ";rewarded=" + string(_rewarded)
            + ";message=" + string(_message);

        // Generic log of every Adivery event
        show_debug_message("Adivery async: " + global.adivery_last_event);

        // Extra-friendly logs for rewarded video prepare & callbacks
        var _type = string(_ev);
        var _is_rewarded_placement = (_placement == string(global.adivery_placement_rewarded));

        if (string_pos("prepare_rewarded_start", _type) > 0) {
            show_debug_message("Adivery: Rewarded prepare START -> " + _placement);
        }
        if (string_pos("prepare_rewarded_got_context", _type) > 0) {
            show_debug_message("Adivery: Rewarded got context -> " + _message);
        }
        if (string_pos("prepare_rewarded_invoked", _type) > 0) {
            show_debug_message("Adivery: Rewarded prepare INVOKED");
        }
        if (string_pos("prepare_rewarded_error", _type) > 0) {
            show_debug_message("Adivery: Rewarded prepare ERROR -> " + _message);
        }

        // Common listener callbacks (method names come from SDK listener)
        if (_is_rewarded_placement) {
            if (_type == "onAdLoaded") show_debug_message("Adivery: Rewarded LOADED");
            if (_type == "onAdShown") show_debug_message("Adivery: Rewarded SHOWN");
            if (_type == "onAdClosed") show_debug_message("Adivery: Rewarded CLOSED");
            if (_type == "onAdError") show_debug_message("Adivery: Rewarded ERROR -> " + _message);
            if (_type == "onRewarded") show_debug_message("Adivery: Reward GRANTED");
        }
    }
}
