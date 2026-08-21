if (current_time - adivery_last_event_poll >= adivery_event_poll_interval) {
    var _evs = adivery_get_last_event();
    adivery_last_event_poll = current_time;

    if (_evs != "") {
        global.adivery_last_event = _evs;

        var _type = "";
        var _placement = "";
        var _message = "";

        var _type_start = string_pos("type=", _evs);
        if (_type_start > 0) {
            var _type_from = _type_start + 5;
            var _type_end = string_pos_ext(";", _evs, _type_from);
            if (_type_end > 0) _type = string_copy(_evs, _type_from, _type_end - _type_from);
            else _type = string_copy(_evs, _type_from, string_length(_evs) - _type_from + 1);
        }

        var _placement_start = string_pos("placement=", _evs);
        if (_placement_start > 0) {
            var _placement_from = _placement_start + 10;
            var _placement_end = string_pos_ext(";", _evs, _placement_from);
            if (_placement_end > 0) _placement = string_copy(_evs, _placement_from, _placement_end - _placement_from);
            else _placement = string_copy(_evs, _placement_from, string_length(_evs) - _placement_from + 1);
        }

        var _message_start = string_pos("message=", _evs);
        if (_message_start > 0) {
            var _message_from = _message_start + 8;
            var _message_end = string_pos_ext(";", _evs, _message_from);
            if (_message_end > 0) _message = string_copy(_evs, _message_from, _message_end - _message_from);
            else _message = string_copy(_evs, _message_from, string_length(_evs) - _message_from + 1);
        }

        if (_type == "prepare_rewarded_error") {
            show_debug_message("Adivery: Rewarded prepare ERROR -> " + _message);
        }

        if (_placement == string(global.adivery_placement_interstitial)) {
            if (_type == "onAdLoaded") adivery_interstitial_ready = 1;
            if (_type == "onAdClosed" || _type == "onAdError") adivery_interstitial_ready = 0;
        }

        if (_placement == string(global.adivery_placement_rewarded)) {
            if (_type == "onAdLoaded") adivery_rewarded_ready = 1;
            if (_type == "onAdClosed" || _type == "onAdError") adivery_rewarded_ready = 0;
            if (_type == "onAdLoaded") show_debug_message("Adivery: Rewarded LOADED");
            if (_type == "onAdShown") show_debug_message("Adivery: Rewarded SHOWN");
            if (_type == "onAdClosed") show_debug_message("Adivery: Rewarded CLOSED");
            if (_type == "onAdError") show_debug_message("Adivery: Rewarded ERROR -> " + _message);
            if (_type == "onRewarded") show_message_async("Thanks! Your reward has been granted.");
        }
    }
}
