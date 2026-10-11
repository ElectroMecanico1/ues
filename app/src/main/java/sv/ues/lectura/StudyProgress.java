package sv.ues.lectura;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;

/** Almacenamiento del avance en Android y compatibilidad con los respaldos JSON de la web. */
final class StudyProgress {
    private static final String PREF = "ues_lectura_native";
    private final SharedPreferences prefs;
    private final StudyRepository repo;
    private JSONObject progress = new JSONObject();
    private JSONArray saved = new JSONArray();
    private JSONObject recent;
    private double size = 1.0;

    StudyProgress(Context context, StudyRepository repository) {
        repo = repository;
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        try { importJson(new JSONObject(prefs.getString("state", "{}")), false); }
        catch (Exception ignored) { save(); }
    }
    private int clamp(int n, int length) { return Math.min(Math.max(0, n), Math.max(0, length - 1)); }
    private boolean valid(String book, String subject) { return repo.validBook(book) && repo.validSubject(subject) && !repo.subject(book, subject).isEmpty(); }

    int last(String book, String subject) {
        JSONObject p = progress.optJSONObject(StudyRepository.key(book, subject));
        return clamp(p == null ? 0 : p.optInt("last"), repo.subject(book, subject).size());
    }
    int seenCount(String book, String subject) {
        JSONObject p = progress.optJSONObject(StudyRepository.key(book, subject));
        JSONArray seen = p == null ? null : p.optJSONArray("seen");
        return seen == null ? 0 : seen.length();
    }
    int seenInBook(String book) {
        int n = 0;
        for (String subject : StudyRepository.SUBJECTS) n += seenCount(book, subject);
        return n;
    }
    void markSeen(String book, String subject, int index) {
        if (!valid(book, subject)) return;
        int i = clamp(index, repo.subject(book, subject).size());
        String key = StudyRepository.key(book, subject);
        JSONObject record = progress.optJSONObject(key);
        if (record == null) record = new JSONObject();
        JSONArray seen = record.optJSONArray("seen");
        if (seen == null) seen = new JSONArray();
        boolean exists = false;
        for (int j = 0; j < seen.length(); j++) if (seen.optInt(j, -1) == i) { exists = true; break; }
        if (!exists) seen.put(i);
        long now = System.currentTimeMillis();
        try {
            record.put("last", i); record.put("seen", seen); record.put("updated", now);
            progress.put(key, record);
            recent = new JSONObject().put("book", book).put("subject", subject).put("index", i).put("updated", now);
        } catch (Exception ignored) { }
        save();
    }
    boolean isSaved(StudyRepository.Question q) {
        for (int i = 0; i < saved.length(); i++) if (q.savedId().equals(saved.optString(i))) return true;
        return false;
    }
    void toggleSaved(StudyRepository.Question q) {
        JSONArray next = new JSONArray();
        boolean wasSaved = isSaved(q);
        for (int i = 0; i < saved.length(); i++) {
            String id = saved.optString(i);
            if (!q.savedId().equals(id)) next.put(id);
        }
        if (!wasSaved) next.put(q.savedId());
        saved = next;
        save();
    }
    int savedCount() { return saved.length(); }
    boolean hasRecent() {
        return recent != null && valid(recent.optString("book"), recent.optString("subject"));
    }
    String recentBook() { return hasRecent() ? recent.optString("book") : null; }
    String recentSubject() { return hasRecent() ? recent.optString("subject") : null; }
    int recentIndex() {
        return hasRecent() ? clamp(recent.optInt("index"), repo.subject(recentBook(), recentSubject()).size()) : 0;
    }
    double fontSize() { return size; }
    void changeFont(double delta) { size = Math.max(0.90, Math.min(1.35, Math.round((size + delta) * 100.0) / 100.0)); save(); }
    JSONArray savedIds() { return saved; }

    JSONObject exportJson() {
        JSONObject value = new JSONObject();
        try {
            value.put("app", "UES Lectura");
            value.put("version", 2);
            value.put("savedAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(new java.util.Date()));
            value.put("progress", progress);
            value.put("saved", saved);
            value.put("size", size);
            value.put("recent", recent == null ? JSONObject.NULL : recent);
        } catch (Exception ignored) { }
        return value;
    }

    void importJson(JSONObject source, boolean validateApp) throws Exception {
        if (validateApp && !"UES Lectura".equals(source.optString("app"))) throw new Exception("Archivo no válido para UES Lectura");
        JSONObject p = source.optJSONObject("progress");
        JSONArray list = source.optJSONArray("saved");
        if (validateApp && (p == null || list == null)) throw new Exception("El respaldo no contiene datos de progreso válidos");
        if (p == null) p = new JSONObject();
        if (list == null) list = new JSONArray();
        JSONObject clean = new JSONObject();
        for (String book : StudyRepository.BOOK_IDS) {
            for (String subject : StudyRepository.SUBJECTS) {
                if (!valid(book, subject)) continue;
                String key = StudyRepository.key(book, subject);
                JSONObject record = p.optJSONObject(key);
                if (record == null) continue;
                int length = repo.subject(book, subject).size();
                int last = clamp(record.optInt("last"), length);
                JSONArray orig = record.optJSONArray("seen");
                JSONArray seen = new JSONArray();
                HashSet<Integer> ids = new HashSet<>();
                if (orig != null) for (int i = 0; i < orig.length(); i++) {
                    int item = orig.optInt(i, -1);
                    if (item >= 0 && item < length && ids.add(item)) seen.put(item);
                }
                JSONObject entry = new JSONObject();
                entry.put("last", last);
                entry.put("seen", seen);
                entry.put("updated", record.optLong("updated", 0L));
                clean.put(key, entry);
            }
        }
        JSONArray cleanSaved = new JSONArray();
        HashSet<String> unique = new HashSet<>();
        for (int i = 0; i < list.length(); i++) {
            String id = list.optString(i);
            if (repo.validSavedId(id) && unique.add(id)) cleanSaved.put(id);
        }
        JSONObject rec = source.optJSONObject("recent");
        if (rec != null && !valid(rec.optString("book"), rec.optString("subject"))) rec = null;
        if (rec != null) {
            int index = clamp(rec.optInt("index"), repo.subject(rec.optString("book"), rec.optString("subject")).size());
            rec.put("index", index);
        } else {
            long newest = -1;
            for (String book : StudyRepository.BOOK_IDS) {
                for (String subject : StudyRepository.SUBJECTS) {
                    JSONObject record = clean.optJSONObject(StudyRepository.key(book, subject));
                    if (record != null && record.optLong("updated", 0) > newest) {
                        newest = record.optLong("updated", 0);
                        rec = new JSONObject().put("book", book).put("subject", subject)
                                .put("index", record.optInt("last")).put("updated", newest);
                    }
                }
            }
        }
        progress = clean;
        saved = cleanSaved;
        recent = rec;
        double requested = source.optDouble("size", 1.0);
        size = Double.isNaN(requested) ? 1.0 : Math.max(.90, Math.min(1.35, requested));
        save();
    }

    private void save() { prefs.edit().putString("state", exportJson().toString()).apply(); }
}
