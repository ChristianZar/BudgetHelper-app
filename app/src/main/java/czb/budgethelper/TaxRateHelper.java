package czb.budgethelper;

import java.util.HashMap;

/**
 * Offline lookup: US ZIP code (3-digit prefix) → state → combined average sales tax rate.
 * Rates are combined state + average local tax (Tax Foundation data).
 */
public class TaxRateHelper {

    private static final HashMap<String, String> ZIP_TO_STATE = new HashMap<>();
    private static final HashMap<String, Double> STATE_TAX_RATES = new HashMap<>();

    static {
        // ── State average combined sales tax rates ──────────────────────────
        STATE_TAX_RATES.put("AL", 0.0922); STATE_TAX_RATES.put("AK", 0.0176);
        STATE_TAX_RATES.put("AZ", 0.0840); STATE_TAX_RATES.put("AR", 0.0951);
        STATE_TAX_RATES.put("CA", 0.0875); STATE_TAX_RATES.put("CO", 0.0777);
        STATE_TAX_RATES.put("CT", 0.0635); STATE_TAX_RATES.put("DE", 0.0000);
        STATE_TAX_RATES.put("FL", 0.0700); STATE_TAX_RATES.put("GA", 0.0736);
        STATE_TAX_RATES.put("HI", 0.0444); STATE_TAX_RATES.put("ID", 0.0603);
        STATE_TAX_RATES.put("IL", 0.0882); STATE_TAX_RATES.put("IN", 0.0700);
        STATE_TAX_RATES.put("IA", 0.0694); STATE_TAX_RATES.put("KS", 0.0869);
        STATE_TAX_RATES.put("KY", 0.0600); STATE_TAX_RATES.put("LA", 0.0955);
        STATE_TAX_RATES.put("ME", 0.0550); STATE_TAX_RATES.put("MD", 0.0600);
        STATE_TAX_RATES.put("MA", 0.0625); STATE_TAX_RATES.put("MI", 0.0600);
        STATE_TAX_RATES.put("MN", 0.0752); STATE_TAX_RATES.put("MS", 0.0707);
        STATE_TAX_RATES.put("MO", 0.0825); STATE_TAX_RATES.put("MT", 0.0000);
        STATE_TAX_RATES.put("NE", 0.0694); STATE_TAX_RATES.put("NV", 0.0823);
        STATE_TAX_RATES.put("NH", 0.0000); STATE_TAX_RATES.put("NJ", 0.0660);
        STATE_TAX_RATES.put("NM", 0.0783); STATE_TAX_RATES.put("NY", 0.0852);
        STATE_TAX_RATES.put("NC", 0.0698); STATE_TAX_RATES.put("ND", 0.0696);
        STATE_TAX_RATES.put("OH", 0.0722); STATE_TAX_RATES.put("OK", 0.0898);
        STATE_TAX_RATES.put("OR", 0.0000); STATE_TAX_RATES.put("PA", 0.0634);
        STATE_TAX_RATES.put("RI", 0.0700); STATE_TAX_RATES.put("SC", 0.0746);
        STATE_TAX_RATES.put("SD", 0.0640); STATE_TAX_RATES.put("TN", 0.0955);
        STATE_TAX_RATES.put("TX", 0.0825); STATE_TAX_RATES.put("UT", 0.0719);
        STATE_TAX_RATES.put("VT", 0.0624); STATE_TAX_RATES.put("VA", 0.0578);
        STATE_TAX_RATES.put("WA", 0.0990); STATE_TAX_RATES.put("WV", 0.0650);
        STATE_TAX_RATES.put("WI", 0.0543); STATE_TAX_RATES.put("WY", 0.0536);
        STATE_TAX_RATES.put("DC", 0.0600);

        // ── ZIP 3-digit prefix → state ───────────────────────────────────────
        // AL 350-369
        for (int i = 350; i <= 369; i++) ZIP_TO_STATE.put(String.valueOf(i), "AL");
        // AK 995-999
        for (int i = 995; i <= 999; i++) ZIP_TO_STATE.put(String.valueOf(i), "AK");
        // AZ 850-865
        for (int i = 850; i <= 865; i++) ZIP_TO_STATE.put(String.valueOf(i), "AZ");
        // AR 716-729
        for (int i = 716; i <= 729; i++) ZIP_TO_STATE.put(String.valueOf(i), "AR");
        // CA 900-961
        for (int i = 900; i <= 961; i++) ZIP_TO_STATE.put(String.valueOf(i), "CA");
        // CO 800-816
        for (int i = 800; i <= 816; i++) ZIP_TO_STATE.put(String.valueOf(i), "CO");
        // CT 060-069
        for (int i = 60; i <= 69; i++) ZIP_TO_STATE.put(String.format("%03d", i), "CT");
        // DE 197-199
        for (int i = 197; i <= 199; i++) ZIP_TO_STATE.put(String.valueOf(i), "DE");
        // DC 200-205
        for (int i = 200; i <= 205; i++) ZIP_TO_STATE.put(String.valueOf(i), "DC");
        // FL 320-349
        for (int i = 320; i <= 349; i++) ZIP_TO_STATE.put(String.valueOf(i), "FL");
        // GA 300-319
        for (int i = 300; i <= 319; i++) ZIP_TO_STATE.put(String.valueOf(i), "GA");
        // HI 967-968
        for (int i = 967; i <= 968; i++) ZIP_TO_STATE.put(String.valueOf(i), "HI");
        // ID 832-838
        for (int i = 832; i <= 838; i++) ZIP_TO_STATE.put(String.valueOf(i), "ID");
        // IL 600-629
        for (int i = 600; i <= 629; i++) ZIP_TO_STATE.put(String.valueOf(i), "IL");
        // IN 460-479
        for (int i = 460; i <= 479; i++) ZIP_TO_STATE.put(String.valueOf(i), "IN");
        // IA 500-528
        for (int i = 500; i <= 528; i++) ZIP_TO_STATE.put(String.valueOf(i), "IA");
        // KS 660-679
        for (int i = 660; i <= 679; i++) ZIP_TO_STATE.put(String.valueOf(i), "KS");
        // KY 400-427
        for (int i = 400; i <= 427; i++) ZIP_TO_STATE.put(String.valueOf(i), "KY");
        // LA 700-715
        for (int i = 700; i <= 715; i++) ZIP_TO_STATE.put(String.valueOf(i), "LA");
        // ME 039-049
        for (int i = 39; i <= 49; i++) ZIP_TO_STATE.put(String.format("%03d", i), "ME");
        // MD 206-219
        for (int i = 206; i <= 219; i++) ZIP_TO_STATE.put(String.valueOf(i), "MD");
        // MA 010-027
        for (int i = 10; i <= 27; i++) ZIP_TO_STATE.put(String.format("%03d", i), "MA");
        // MI 480-499
        for (int i = 480; i <= 499; i++) ZIP_TO_STATE.put(String.valueOf(i), "MI");
        // MN 550-567
        for (int i = 550; i <= 567; i++) ZIP_TO_STATE.put(String.valueOf(i), "MN");
        // MS 386-397
        for (int i = 386; i <= 397; i++) ZIP_TO_STATE.put(String.valueOf(i), "MS");
        // MO 630-658
        for (int i = 630; i <= 658; i++) ZIP_TO_STATE.put(String.valueOf(i), "MO");
        // MT 590-599
        for (int i = 590; i <= 599; i++) ZIP_TO_STATE.put(String.valueOf(i), "MT");
        // NE 680-693
        for (int i = 680; i <= 693; i++) ZIP_TO_STATE.put(String.valueOf(i), "NE");
        // NV 889-898
        for (int i = 889; i <= 898; i++) ZIP_TO_STATE.put(String.valueOf(i), "NV");
        // NH 030-038
        for (int i = 30; i <= 38; i++) ZIP_TO_STATE.put(String.format("%03d", i), "NH");
        // NJ 070-089
        for (int i = 70; i <= 89; i++) ZIP_TO_STATE.put(String.format("%03d", i), "NJ");
        // NM 870-884
        for (int i = 870; i <= 884; i++) ZIP_TO_STATE.put(String.valueOf(i), "NM");
        // NY 100-149
        for (int i = 100; i <= 149; i++) ZIP_TO_STATE.put(String.valueOf(i), "NY");
        // NC 270-289
        for (int i = 270; i <= 289; i++) ZIP_TO_STATE.put(String.valueOf(i), "NC");
        // ND 580-588
        for (int i = 580; i <= 588; i++) ZIP_TO_STATE.put(String.valueOf(i), "ND");
        // OH 430-459
        for (int i = 430; i <= 459; i++) ZIP_TO_STATE.put(String.valueOf(i), "OH");
        // OK 730-749
        for (int i = 730; i <= 749; i++) ZIP_TO_STATE.put(String.valueOf(i), "OK");
        // OR 970-979
        for (int i = 970; i <= 979; i++) ZIP_TO_STATE.put(String.valueOf(i), "OR");
        // PA 150-196
        for (int i = 150; i <= 196; i++) ZIP_TO_STATE.put(String.valueOf(i), "PA");
        // RI 028-029
        for (int i = 28; i <= 29; i++) ZIP_TO_STATE.put(String.format("%03d", i), "RI");
        // SC 290-299
        for (int i = 290; i <= 299; i++) ZIP_TO_STATE.put(String.valueOf(i), "SC");
        // SD 570-577
        for (int i = 570; i <= 577; i++) ZIP_TO_STATE.put(String.valueOf(i), "SD");
        // TN 370-385
        for (int i = 370; i <= 385; i++) ZIP_TO_STATE.put(String.valueOf(i), "TN");
        // TX 750-799
        for (int i = 750; i <= 799; i++) ZIP_TO_STATE.put(String.valueOf(i), "TX");
        // UT 840-847
        for (int i = 840; i <= 847; i++) ZIP_TO_STATE.put(String.valueOf(i), "UT");
        // VT 050-059
        for (int i = 50; i <= 59; i++) ZIP_TO_STATE.put(String.format("%03d", i), "VT");
        // VA 220-246
        for (int i = 220; i <= 246; i++) ZIP_TO_STATE.put(String.valueOf(i), "VA");
        // WA 980-994
        for (int i = 980; i <= 994; i++) ZIP_TO_STATE.put(String.valueOf(i), "WA");
        // WV 247-268
        for (int i = 247; i <= 268; i++) ZIP_TO_STATE.put(String.valueOf(i), "WV");
        // WI 530-549
        for (int i = 530; i <= 549; i++) ZIP_TO_STATE.put(String.valueOf(i), "WI");
        // WY 820-831
        for (int i = 820; i <= 831; i++) ZIP_TO_STATE.put(String.valueOf(i), "WY");
    }

    /** Returns the combined average sales tax rate for the given 5-digit US ZIP, or 0.0 if unknown. */
    public static double getTaxRate(String zip) {
        if (zip == null || zip.length() < 3) return 0.0;
        String prefix = zip.substring(0, 3);
        String state = ZIP_TO_STATE.get(prefix);
        if (state == null) return 0.0;
        Double rate = STATE_TAX_RATES.get(state);
        return rate != null ? rate : 0.0;
    }

    /** Returns the state abbreviation for the given ZIP, or empty string if unknown. */
    public static String getState(String zip) {
        if (zip == null || zip.length() < 3) return "";
        String state = ZIP_TO_STATE.get(zip.substring(0, 3));
        return state != null ? state : "";
    }
}
