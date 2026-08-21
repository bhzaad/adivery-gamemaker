/// @descr Adivery init
// Keep SDK logging and logcat scanning off during normal runtime.
adivery_set_logging(0);
adivery_enable_logcat_reward_fallback(0);


// Set your IDs here
global.adivery_app_id = "0cb6fc5f-3412-43cc-bc00-9a441619e105";
global.adivery_placement_interstitial = "c868a543-4704-40d2-9cf3-0c2e7d173b06";
global.adivery_placement_rewarded = "2265842e-f7f1-4bd2-b5a4-6c923037734f";
global.adivery_placement_banner = "854d2a27-32d2-4589-a3e5-1189609e4510";
global.adivery_placement_appopen = "f83ece42-78b8-4bc9-a341-98ba02b0578d";

// Configure SDK
adivery_init(global.adivery_app_id);
adivery_callbacks_enable(1);

// Prepare ads
//adivery_prepare_interstitial(global.adivery_placement_interstitial);
adivery_prepare_rewarded(global.adivery_placement_rewarded);

// State
shown_interstitial = false;
shown_rewarded = false;
shown_appopen = false;
global.adivery_last_event = "";
adivery_interstitial_ready = 0;
adivery_rewarded_ready = 0;
adivery_vast_ready = 0;
adivery_last_event_poll = -1000000;
adivery_event_poll_interval = 250;

// onRewarded rewards callback
reward = function() {};