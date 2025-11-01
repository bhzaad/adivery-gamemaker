/// @descr Adivery init
// Disable verbose SDK logging for runtime builds
adivery_set_logging(1);
adivery_callbacks_enable(1);
adivery_enable_logcat_reward_fallback(1); // Optional: enable debug-only logcat fallback to detect reward via SDK logs


// Set your IDs here
global.adivery_app_id = "0cb6fc5f-3412-43cc-bc00-9a441619e105";
global.adivery_placement_interstitial = "c868a543-4704-40d2-9cf3-0c2e7d173b06";
global.adivery_placement_rewarded = "2265842e-f7f1-4bd2-b5a4-6c923037734f";
global.adivery_placement_banner = "854d2a27-32d2-4589-a3e5-1189609e4510";
global.adivery_placement_appopen = "f83ece42-78b8-4bc9-a341-98ba02b0578d";

// Configure SDK
adivery_init(global.adivery_app_id);

// Prepare ads
//adivery_prepare_interstitial(global.adivery_placement_interstitial);
// Extra debug visibility for rewarded prepare flow
show_debug_message("Adivery: preparing rewarded start -> " + string(global.adivery_placement_rewarded));
adivery_prepare_rewarded(global.adivery_placement_rewarded);
show_debug_message("Adivery: preparing rewarded invoked");

// State
shown_interstitial = false;
shown_rewarded = false;
shown_appopen = false;
global.adivery_last_event = "";