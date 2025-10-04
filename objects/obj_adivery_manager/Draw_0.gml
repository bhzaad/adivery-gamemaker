// On-screen diagnostics for Adivery
var x0 = 16; var y0 = 16; var dy = 18;
draw_set_color(c_white);
draw_text(x0, y0,    "Adivery Diagnostics"); y0 += dy;
draw_text(x0, y0,    "App ID: " + string(global.adivery_app_id)); y0 += dy;
draw_text(x0, y0,    "Interstitial ready: " + string(adivery_is_loaded(global.adivery_placement_interstitial))); y0 += dy;
draw_text(x0, y0,    "Rewarded ready: " + string(adivery_is_loaded(global.adivery_placement_rewarded))); y0 += dy;
draw_text(x0, y0,    "VAST ready: " + string(adivery_is_vast_ready())); y0 += dy;
draw_text(x0, y0,    "Last event: " + string(global.adivery_last_event)); y0 += dy;

// Simple buttons to show ads
var btn_w = 220, btn_h = 28;

// Interstitial button
var i_x1 = x0, i_y1 = y0 + 8, i_x2 = x0 + btn_w, i_y2 = i_y1 + btn_h;
draw_set_color(make_color_rgb(40,40,40));
draw_rectangle(i_x1, i_y1, i_x2, i_y2, false);
draw_set_color(c_white);
draw_text(i_x1 + 8, i_y1 + 6, "Show Interstitial");

// Rewarded button
var r_x1 = x0, r_y1 = i_y2 + 8, r_x2 = x0 + btn_w, r_y2 = r_y1 + btn_h;
draw_set_color(make_color_rgb(40,40,40));
draw_rectangle(r_x1, r_y1, r_x2, r_y2, false);
draw_set_color(c_white);
draw_text(r_x1 + 8, r_y1 + 6, "Show Rewarded");

// Banner show/hide buttons
var b_x1 = x0, b_y1 = r_y2 + 8, b_x2 = x0 + btn_w, b_y2 = b_y1 + btn_h;
draw_set_color(make_color_rgb(40,40,40));
draw_rectangle(b_x1, b_y1, b_x2, b_y2, false);
draw_set_color(c_white);
draw_text(b_x1 + 8, b_y1 + 6, "Show Banner (bottom)");

var bh_x1 = x0, bh_y1 = b_y2 + 8, bh_x2 = x0 + btn_w, bh_y2 = bh_y1 + btn_h;
draw_set_color(make_color_rgb(40,40,40));
draw_rectangle(bh_x1, bh_y1, bh_x2, bh_y2, false);
draw_set_color(c_white);
draw_text(bh_x1 + 8, bh_y1 + 6, "App Open"); // Hide Banner

// Click handling (room-space coordinates)
if (mouse_check_button_pressed(mb_left)) {
    if (point_in_rectangle(mouse_x, mouse_y, i_x1, i_y1, i_x2, i_y2)) {
        if (adivery_is_loaded(global.adivery_placement_interstitial) == 1) {
            adivery_show(global.adivery_placement_interstitial);
        } else {
            show_debug_message("Adivery: interstitial not ready");
            adivery_prepare_interstitial(global.adivery_placement_interstitial);
        }
    }
    if (point_in_rectangle(mouse_x, mouse_y, r_x1, r_y1, r_x2, r_y2)) {
        if (adivery_is_loaded(global.adivery_placement_rewarded) == 1) {
            adivery_show(global.adivery_placement_rewarded);
        } else {
            show_debug_message("Adivery: rewarded not ready");
            adivery_prepare_rewarded(global.adivery_placement_rewarded);
        }
    }
    if (point_in_rectangle(mouse_x, mouse_y, b_x1, b_y1, b_x2, b_y2)) {
        adivery_banner_show(global.adivery_placement_banner, "BANNER", 1);
    }
    if (point_in_rectangle(mouse_x, mouse_y, bh_x1, bh_y1, bh_x2, bh_y2)) {
        // adivery_banner_hide();
		adivery_prepare_app_open(global.adivery_placement_appopen);
    }
}