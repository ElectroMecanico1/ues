package sv.ues.lectura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Interfaz 100 % Android: usa TextView, botones, vistas y SharedPreferences. No WebView. */
public final class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 810, IMPORT_REQUEST = 811;
    private static final int NAVY = Color.rgb(14, 38, 53);
    private static final int TEAL = Color.rgb(5, 126, 106);
    private static final int BG = Color.rgb(244, 248, 248);
    private static final int MUTED = Color.rgb(99, 116, 127);
    private static final int WHITE = Color.WHITE;
    private static final int SOFT_GREEN = Color.rgb(229, 248, 239);
    private static final int GREEN = Color.rgb(17, 111, 78);
    private static final int[] COLORS = {0xff087f70, 0xff3d9475, 0xff517ebb, 0xff9360b5, 0xffbe803b, 0xffcc7072, 0xff9870b6};
    private static final String[] SHORTS = {"CS", "BIO", "FIS", "QUI", "Σ", "LEN", "LIT"};

    private StudyRepository data;
    private StudyProgress progress;
    private LinearLayout frame, content, bottom;
    private ScrollView scroll;
    private String page = "home", book = "bank", subject = "Matemática";
    private int index = 0;
    private TextToSpeech speaker;
    private boolean speaking = false;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        try {
            data = new StudyRepository(this);
            progress = new StudyProgress(this, data);
        } catch (Exception error) {
            new AlertDialog.Builder(this).setTitle("No se pueden cargar las preguntas")
                    .setMessage(error.getMessage()).setPositiveButton("Cerrar", (dialog, which) -> finish()).show();
            return;
        }
        if (state != null) {
            page = state.getString("page", "home");
            book = state.getString("book", "bank");
            subject = state.getString("subject", "Matemática");
            index = state.getInt("index", 0);
        }
        try {
            speaker = new TextToSpeech(this, result -> {
                if (result == TextToSpeech.SUCCESS && speaker != null) {
                    int installed = speaker.setLanguage(new Locale("es", "SV"));
                    if (installed == TextToSpeech.LANG_MISSING_DATA || installed == TextToSpeech.LANG_NOT_SUPPORTED)
                        speaker.setLanguage(new Locale("es"));
                    speaker.setSpeechRate(.92f);
                    speaker.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override public void onStart(String utteranceId) { }
                        @Override public void onDone(String utteranceId) { runOnUiThread(() -> speaking = false); }
                        @Override public void onError(String utteranceId) { runOnUiThread(() -> speaking = false); }
                    });
                }
            });
        } catch (Exception ignored) { speaker = null; }
        render();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("page", page);
        out.putString("book", book);
        out.putString("subject", subject);
        out.putInt("index", index);
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        stopSpeech();
        if (speaker != null) speaker.shutdown();
        super.onDestroy();
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable shape(int color, int radius, int border, int width) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        if (width > 0) bg.setStroke(dp(width), border);
        return bg;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams fill() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams space(int top) { LinearLayout.LayoutParams p = fill(); p.topMargin = dp(top); return p; }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private LinearLayout.LayoutParams vweight() { return new LinearLayout.LayoutParams(-1, 0, 1); }
    private void pad(View v, int x, int y) { v.setPadding(dp(x), dp(y), dp(x), dp(y)); }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setIncludeFontPadding(true);
        v.setLineSpacing(dp(3), 1.09f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private TextView chip(String text, int color, int bg) {
        TextView v = text(text, 11, color, true);
        v.setBackground(shape(bg, 10, 0, 0));
        pad(v, 10, 7);
        return v;
    }
    private Button button(String label, boolean primary, Runnable click) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinimumHeight(dp(50));
        b.setMinHeight(dp(50));
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setTextColor(primary ? WHITE : NAVY);
        b.setBackground(shape(primary ? TEAL : 0xffeaf0f2, 13, 0, 0));
        b.setOnClickListener(v -> { if (click != null) click.run(); });
        return b;
    }
    private LinearLayout card(int color) {
        LinearLayout v = column();
        v.setBackground(shape(color, 20, 0xffedf2f4, color == WHITE ? 1 : 0));
        pad(v, 20, 19);
        return v;
    }
    private LinearLayout rounded(int color) {
        LinearLayout v = column();
        v.setBackground(shape(color, 14, 0, 0));
        pad(v, 13, 13);
        return v;
    }
    private int subjectIndex(String s) {
        for (int i = 0; i < StudyRepository.SUBJECTS.length; i++) if (StudyRepository.SUBJECTS[i].equals(s)) return i;
        return 0;
    }
    private String bookName(String b) {
        for (int i = 0; i < StudyRepository.BOOK_IDS.length; i++) if (StudyRepository.BOOK_IDS[i].equals(b)) return StudyRepository.BOOK_NAMES[i];
        return b;
    }
    private int bookColor(String b) { return "sim".equals(b) ? 0xff7467ae : "official".equals(b) ? 0xffb77a3e : TEAL; }
    private void stopSpeech() { if (speaker != null) try { speaker.stop(); } catch (Exception ignored) {} speaking = false; }
    private void navigate(String next) { stopSpeech(); page = next; render(); }
    private void openBook(String b) { book = b; navigate("subjects"); }
    private void openReader(String b, String s, int position) {
        book = b; subject = s;
        List<StudyRepository.Question> list = data.subject(book, subject);
        if (list.isEmpty()) return;
        index = Math.min(list.size()-1, Math.max(0, position));
        navigate("reader");
    }
    private void resume() { if (progress.hasRecent()) openReader(progress.recentBook(), progress.recentSubject(), progress.recentIndex()); }
    private void goBack() {
        if ("reader".equals(page) || "search".equals(page)) navigate("subjects");
        else if ("home".equals(page)) super.onBackPressed();
        else navigate("home");
    }
    @Override public void onBackPressed() { goBack(); }

    private void render() {
        frame = column();
        frame.setBackgroundColor(BG);
        frame.setFitsSystemWindows(true);
        setContentView(frame);
        renderHeader();
        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        content = column();
        content.setPadding(dp(18), dp(20), dp(18), dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        frame.addView(scroll, vweight());
        switch (page) {
            case "subjects": renderSubjects(); break;
            case "reader": renderReader(); break;
            case "search": renderSearch(); break;
            case "saved": renderSaved(); break;
            default: renderHome();
        }
        if ("reader".equals(page)) renderBottom();
    }
    private void renderHeader() {
        LinearLayout bar = row();
        bar.setBackgroundColor(WHITE);
        bar.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (!"home".equals(page)) {
            Button back = button("‹", false, this::goBack);
            back.setContentDescription("Volver a la pantalla anterior");
            bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        LinearLayout brand = column();
        TextView title = text("UES Lectura", 18, NAVY, true);
        TextView mini = text("REPASO SIN INTERNET", 10, TEAL, true);
        brand.addView(title); brand.addView(mini);
        LinearLayout.LayoutParams bp = weight(); bp.leftMargin = dp(10);
        bar.addView(brand, bp);
        if (!"home".equals(page)) {
            Button home = button("⌂", false, () -> navigate("home"));
            home.setContentDescription("Ir al inicio");
            bar.addView(home, new LinearLayout.LayoutParams(dp(46), dp(44)));
        }
        Button fav = button("★ " + progress.savedCount(), false, () -> navigate("saved"));
        fav.setContentDescription("Preguntas guardadas: " + progress.savedCount());
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-2, dp(44)); fp.leftMargin = dp(5);
        bar.addView(fav, fp);
        frame.addView(bar, fill());
    }

    private void renderHome() {
        LinearLayout hero = card(NAVY);
        hero.addView(chip("TU ESPACIO DE REPASO", 0xffaff6db, 0xff244959));
        TextView slogan = text("Una pregunta.\nUna respuesta.\nA tu ritmo.", 27, WHITE, true);
        hero.addView(slogan, space(14));
        hero.addView(text("Estudiá desde tu celular: respuestas claras, Matemática desarrollada y avance automático.", 14, 0xffd2e2e7, false), space(9));
        content.addView(hero);
        if (progress.hasRecent()) {
            LinearLayout c = card(0xffe2f4ee);
            c.addView(text("↺  CONTINUÁ DONDE TE QUEDASTE", 12, GREEN, true));
            c.addView(text(bookName(progress.recentBook()) + " · " + progress.recentSubject(), 16, NAVY, true), space(10));
            c.addView(text("Pregunta " + (progress.recentIndex()+1) + " de " + data.subject(progress.recentBook(), progress.recentSubject()).size(), 13, MUTED, false), space(5));
            c.addView(button("Retomar mi lectura  →", true, this::resume), space(12));
            content.addView(c, space(15));
        }
        content.addView(text("Elegí qué querés estudiar", 21, NAVY, true), space(25));
        content.addView(text("Tres colecciones · 650 fichas de repaso", 13, MUTED, false), space(5));
        String[] desc = {
            "Repasá una selección amplia de los temarios, organizada por siete materias.",
            "Un simulacro con 100 preguntas y sus respuestas correctas.",
            "Referencia histórica de 2016 con respuestas académicamente revisadas."
        };
        for (int i = 0; i < StudyRepository.BOOK_IDS.length; i++) {
            final String id = StudyRepository.BOOK_IDS[i];
            LinearLayout c = card(WHITE);
            c.addView(chip("COLECCIÓN 0" + (i + 1), bookColor(id), 0xffedf5f4));
            c.addView(text(StudyRepository.BOOK_NAMES[i], 20, NAVY, true), space(13));
            c.addView(text(desc[i], 13, MUTED, false), space(7));
            LinearLayout foot = row();
            foot.addView(text(StudyRepository.COUNTS[i] + " preguntas", 13, bookColor(id), true), weight());
            foot.addView(text("Explorar →", 14, TEAL, true));
            c.addView(foot, space(17));
            c.setOnClickListener(v -> openBook(id));
            c.setClickable(true);
            c.setContentDescription("Abrir colección " + StudyRepository.BOOK_NAMES[i]);
            content.addView(c, space(13));
        }
        LinearLayout backup = card(0xffeaf0f3);
        backup.addView(text("Tu estudio, siempre guardado", 17, NAVY, true));
        backup.addView(text("El avance se guarda en este Android. También podés importar la copia de tu anterior aplicación HTML.", 13, MUTED, false), space(7));
        backup.addView(button("Exportar mi progreso", false, this::exportProgress), space(12));
        backup.addView(button("Importar progreso de HTML", false, this::importProgress), space(8));
        content.addView(backup, space(20));
        content.addView(text("Material de práctica; no es una aplicación oficial de la UES ni predice las preguntas reales.", 12, MUTED, false), space(18));
    }

    private void renderSubjects() {
        content.addView(chip("MATERIAS", TEAL, 0xffe4f5ef));
        content.addView(text(bookName(book), 25, NAVY, true), space(13));
        content.addView(text("Elegí una materia. Retomaremos exactamente la pregunta que leíste la última vez.", 14, MUTED, false), space(7));
        for (String s : StudyRepository.SUBJECTS) {
            List<StudyRepository.Question> items = data.subject(book, s);
            if (items.isEmpty()) continue;
            int si = subjectIndex(s);
            int count = progress.seenCount(book, s);
            int last = progress.last(book, s);
            LinearLayout c = card(WHITE);
            LinearLayout r = row();
            TextView icon = text(SHORTS[si], 14, WHITE, true);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(shape(COLORS[si], 14, 0, 0));
            r.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
            LinearLayout meta = column();
            meta.addView(text(s, 17, NAVY, true));
            meta.addView(text(items.size() + " preguntas  ·  " + (count == 0 ? "Sin empezar" : "Retomar en " + (last + 1)), 12, MUTED, false), space(4));
            LinearLayout.LayoutParams mp = weight(); mp.leftMargin = dp(13);
            r.addView(meta, mp);
            r.addView(text("›", 28, TEAL, true));
            c.addView(r);
            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(items.size()); pb.setProgress(count);
            pb.setProgressTintList(ColorStateList.valueOf(COLORS[si]));
            pb.setProgressBackgroundTintList(ColorStateList.valueOf(0xffeaf0f2));
            c.addView(pb, space(14));
            c.setClickable(true);
            c.setContentDescription(s + ": " + items.size() + " preguntas, " + count + " vistas. Continuar.");
            c.setOnClickListener(v -> openReader(book, s, last));
            content.addView(c, space(12));
        }
        content.addView(button("⌕  Buscar una pregunta", false, () -> navigate("search")), space(18));
        content.addView(text(progress.seenInBook(book) + " tarjetas vistas en esta colección", 12, MUTED, false), space(10));
    }

    private void renderReader() {
        List<StudyRepository.Question> items = data.subject(book, subject);
        if (items.isEmpty()) { openBook(book); return; }
        index = Math.max(0, Math.min(items.size() - 1, index));
        progress.markSeen(book, subject, index);
        StudyRepository.Question q = items.get(index);
        int si = subjectIndex(subject);
        LinearLayout top = row();
        top.addView(chip(subject.toUpperCase(Locale.ROOT), COLORS[si], 0xffe5f2f1), weight());
        top.addView(text((index + 1) + " / " + items.size(), 13, MUTED, true));
        content.addView(top);
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(items.size()); pb.setProgress(index + 1);
        pb.setProgressTintList(ColorStateList.valueOf(TEAL));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(0xffdeebeb));
        content.addView(pb, space(13));
        content.addView(text(bookName(book) + " · Pregunta original " + q.n, 12, MUTED, false), space(11));

        LinearLayout qcard = card(WHITE);
        qcard.addView(text("PREGUNTA", 11, MUTED, true));
        qcard.addView(text(q.question, (float)(21 * progress.fontSize()), NAVY, true), space(14));
        if (q.topic != null && !q.topic.trim().isEmpty())
            qcard.addView(text(q.topic, 12, MUTED, false), space(17));
        content.addView(qcard, space(17));

        LinearLayout answer = card(SOFT_GREEN);
        answer.addView(text("✓  RESPUESTA CORRECTA", 12, GREEN, true));
        answer.addView(text(q.answer, (float)(21 * progress.fontSize()), GREEN, true), space(12));
        content.addView(answer, space(13));
        if ("Matemática".equals(subject) && !q.steps.isEmpty()) {
            LinearLayout maths = card(WHITE);
            maths.addView(text("DESARROLLO PASO A PASO", 12, TEAL, true));
            for (int i = 0; i < q.steps.size(); i++) {
                LinearLayout r = row();
                TextView tag = text("" + (i + 1), 13, WHITE, true);
                tag.setGravity(Gravity.CENTER);
                tag.setBackground(shape(TEAL, 20, 0, 0));
                r.addView(tag, new LinearLayout.LayoutParams(dp(30), dp(30)));
                TextView step = text(q.steps.get(i), (float)(15 * progress.fontSize()), NAVY, false);
                LinearLayout.LayoutParams p = weight(); p.leftMargin = dp(11);
                r.addView(step, p);
                maths.addView(r, space(17));
            }
            content.addView(maths, space(14));
        }
        LinearLayout toolbar = row();
        Button star = button(progress.isSaved(q) ? "★ Guardada" : "☆ Guardar", false, () -> {
            progress.toggleSaved(q); render();
        });
        toolbar.addView(star, weight());
        Button sound = button(speaking ? "■ Detener" : "♫ Escuchar", false, () -> speak(q));
        LinearLayout.LayoutParams t = weight(); t.leftMargin = dp(8); toolbar.addView(sound, t);
        content.addView(toolbar, space(14));
        LinearLayout sizes = row();
        sizes.addView(text("Tamaño de letra", 13, MUTED, false), weight());
        Button smaller = button("A−", false, () -> { progress.changeFont(-.05); render(); });
        Button bigger = button("A+", false, () -> { progress.changeFont(.05); render(); });
        sizes.addView(smaller, new LinearLayout.LayoutParams(dp(64), dp(47)));
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(dp(64), dp(47)); b.leftMargin = dp(7);
        sizes.addView(bigger, b);
        content.addView(sizes, space(17));
        content.addView(text("También podés deslizar horizontalmente para avanzar o retroceder.", 12, MUTED, false), space(12));

        GestureDetector detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX(), dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > dp(85) && Math.abs(dx) > Math.abs(dy) * 1.3f && Math.abs(velocityX) > dp(140)) {
                    move(dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
        scroll.setOnTouchListener((view, event) -> { detector.onTouchEvent(event); return false; });
    }
    private void renderBottom() {
        List<StudyRepository.Question> items = data.subject(book, subject);
        bottom = row();
        bottom.setBackgroundColor(WHITE);
        bottom.setPadding(dp(18), dp(10), dp(18), dp(12));
        Button prev = button("‹ Anterior", false, () -> move(-1));
        prev.setEnabled(index > 0);
        Button next = button("Siguiente ›", true, () -> move(1));
        next.setEnabled(index + 1 < items.size());
        bottom.addView(prev, weight());
        LinearLayout.LayoutParams np = weight(); np.leftMargin = dp(10);
        bottom.addView(next, np);
        frame.addView(bottom, fill());
    }
    private void move(int delta) {
        int proposed = index + delta;
        if (proposed < 0 || proposed >= data.subject(book, subject).size()) return;
        index = proposed;
        stopSpeech();
        render();
    }
    private void speak(StudyRepository.Question q) {
        if (speaker == null) { Toast.makeText(this, "La lectura de voz no está disponible", Toast.LENGTH_SHORT).show(); return; }
        if (speaking) { stopSpeech(); render(); return; }
        StringBuilder toRead = new StringBuilder("Pregunta. " + q.question + ". Respuesta correcta. " + q.answer);
        if ("Matemática".equals(q.subject)) for (String step : q.steps) toRead.append(". ").append(step);
        speaking = true;
        speaker.speak(toRead.toString(), TextToSpeech.QUEUE_FLUSH, null, "ues-pregunta");
        Toast.makeText(this, "Leyendo pregunta y respuesta…", Toast.LENGTH_SHORT).show();
    }

    private void renderSaved() {
        content.addView(text("Tus preguntas guardadas", 24, NAVY, true));
        content.addView(text("Volvé a las tarjetas importantes sin buscar entre las colecciones.", 13, MUTED, false), space(7));
        int count = 0;
        for (String b : StudyRepository.BOOK_IDS) {
            for (StudyRepository.Question q : data.collection(b)) {
                if (progress.isSaved(q)) { addResultCard(q); count++; }
            }
        }
        if (count == 0) {
            LinearLayout empty = card(WHITE);
            empty.addView(text("☆ Aún no tenés preguntas guardadas", 18, NAVY, true));
            empty.addView(text("Tocá ☆ Guardar cuando estés leyendo una respuesta que querés repasar.", 14, MUTED, false), space(10));
            content.addView(empty, space(20));
        }
    }
    private void renderSearch() {
        content.addView(text("Buscar preguntas", 24, NAVY, true));
        content.addView(text(bookName(book) + " · Buscá por palabra, respuesta o subtema.", 13, MUTED, false), space(6));
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setTextSize(16);
        query.setHint("Ej. fotosíntesis, democracia, ecuación…");
        query.setBackground(shape(WHITE, 14, 0xffdbe6e7, 1));
        pad(query, 14, 15);
        query.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        content.addView(query, space(17));
        LinearLayout results = column();
        content.addView(results, space(8));
        results.addView(text("Escribí algo para buscar en esta colección.", 14, MUTED, false));
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { showSearchResults(results, s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    private void showSearchResults(LinearLayout target, String input) {
        target.removeAllViews();
        String needle = input.trim().toLowerCase(new Locale("es"));
        if (needle.isEmpty()) {
            target.addView(text("Escribí algo para buscar en esta colección.", 14, MUTED, false));
            return;
        }
        int count = 0;
        for (StudyRepository.Question q : data.collection(book)) {
            String hay = (q.question + " " + q.answer + " " + q.topic + " " + q.subject).toLowerCase(new Locale("es"));
            if (hay.contains(needle)) {
                addResultCard(target, q);
                if (++count == 50) break;
            }
        }
        if (count == 0) target.addView(text("No encontramos preguntas con ese texto.", 14, MUTED, false));
        else if (count == 50) target.addView(text("Mostrando los primeros 50 resultados. Escribí más letras para refinar.", 13, MUTED, false), space(12));
    }
    private void addResultCard(StudyRepository.Question q) { addResultCard(content, q); }
    private void addResultCard(LinearLayout parent, StudyRepository.Question q) {
        LinearLayout c = card(WHITE);
        c.addView(text(bookName(q.book) + " · " + q.subject + " · Nº " + q.n, 11, TEAL, true));
        c.addView(text(q.question, 15, NAVY, true), space(9));
        c.addView(text("✓ " + q.answer, 13, GREEN, true), space(9));
        c.setClickable(true);
        c.setOnClickListener(v -> openReader(q.book, q.subject, data.indexOf(q)));
        parent.addView(c, space(11));
    }

    private void exportProgress() {
        Intent out = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        out.addCategory(Intent.CATEGORY_OPENABLE);
        out.setType("application/json");
        out.putExtra(Intent.EXTRA_TITLE, "UES_Lectura_Avance.json");
        try { startActivityForResult(out, EXPORT_REQUEST); }
        catch (Exception e) { Toast.makeText(this, "No se puede abrir el selector de archivos", Toast.LENGTH_LONG).show(); }
    }
    private void importProgress() {
        Intent in = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        in.addCategory(Intent.CATEGORY_OPENABLE);
        in.setType("application/json");
        try { startActivityForResult(in, IMPORT_REQUEST); }
        catch (Exception e) { Toast.makeText(this, "No se puede abrir el selector de archivos", Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onActivityResult(int request, int result, Intent resultData) {
        super.onActivityResult(request, result, resultData);
        if (result != RESULT_OK || resultData == null || resultData.getData() == null) return;
        Uri uri = resultData.getData();
        if (request == EXPORT_REQUEST) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("No se puede escribir el archivo");
                out.write(progress.exportJson().toString(2).getBytes(StandardCharsets.UTF_8));
                out.flush();
                Toast.makeText(this, "Copia de seguridad guardada", Toast.LENGTH_LONG).show();
            } catch (Exception e) { Toast.makeText(this, "Error guardando copia: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        }
        if (request == IMPORT_REQUEST) {
            try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) throw new Exception("No se puede leer el archivo");
                byte[] buf = new byte[8192]; int n, total = 0;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > 2_000_000) throw new Exception("Archivo demasiado grande");
                    out.write(buf, 0, n);
                }
                final JSONObject imported = new JSONObject(out.toString("UTF-8"));
                if (!"UES Lectura".equals(imported.optString("app"))) throw new Exception("El archivo no es un respaldo de UES Lectura");
                new AlertDialog.Builder(this).setTitle("Recuperar tu avance")
                        .setMessage("Esto reemplazará el avance y las preguntas guardadas de este Android. ¿Querés continuar?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Importar", (dialog, which) -> {
                            try { progress.importJson(imported, true); navigate("home"); Toast.makeText(this, "¡Avance importado correctamente!", Toast.LENGTH_LONG).show(); }
                            catch (Exception e) { Toast.makeText(this, "No se pudo importar: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
                        }).show();
            } catch (Exception e) { Toast.makeText(this, "Error leyendo copia: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
        }
    }
}
