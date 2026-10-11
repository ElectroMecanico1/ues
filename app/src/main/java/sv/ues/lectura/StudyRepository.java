package sv.ues.lectura;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Datos locales. No usa red, ni recopila datos del usuario. */
final class StudyRepository {
    static final String[] BOOK_IDS = {"bank", "sim", "official"};
    static final String[] BOOK_NAMES = {"450 preguntas", "100 preguntas", "Examen UES 2016"};
    static final String[] SUBJECTS = {"Ciencias Sociales", "Biología", "Física", "Química", "Matemática", "Lenguaje", "Literatura"};
    static final int[] COUNTS = {450, 100, 100};
    final Map<String, List<Question>> books = new HashMap<>();
    private final Map<String, List<Question>> bySubject = new HashMap<>();

    static class Question {
        final int n;
        final String book, subject, topic, question, answer;
        final List<String> steps;
        Question(String b, JSONObject o) {
            book = b;
            n = o.optInt("n");
            subject = o.optString("subject");
            topic = o.optString("topic");
            question = o.optString("question");
            answer = o.optString("answer");
            ArrayList<String> list = new ArrayList<>();
            JSONArray array = o.optJSONArray("steps");
            if (array != null) for (int i = 0; i < array.length(); i++) list.add(array.optString(i));
            steps = Collections.unmodifiableList(list);
        }
        String savedId() { return book + ":" + n; }
    }

    StudyRepository(Context context) throws Exception {
        byte[] bytes;
        try (InputStream in = context.getAssets().open("preguntas.json");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] block = new byte[8192];
            int n;
            while ((n = in.read(block)) != -1) out.write(block, 0, n);
            bytes = out.toByteArray();
        }
        JSONObject data = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        for (int b = 0; b < BOOK_IDS.length; b++) {
            String id = BOOK_IDS[b];
            JSONArray array = data.getJSONArray(id);
            ArrayList<Question> questions = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                Question q = new Question(id, array.getJSONObject(i));
                if (q.question.isEmpty() || q.answer.isEmpty() || q.n <= 0) {
                    throw new JSONException("Pregunta incompleta en " + id + " #" + i);
                }
                questions.add(q);
            }
            if (questions.size() != COUNTS[b]) throw new JSONException("Número de preguntas inesperado: " + id);
            books.put(id, Collections.unmodifiableList(questions));
            for (String subject : SUBJECTS) {
                ArrayList<Question> matches = new ArrayList<>();
                for (Question q : questions) if (subject.equals(q.subject)) matches.add(q);
                bySubject.put(key(id, subject), Collections.unmodifiableList(matches));
            }
        }
    }

    List<Question> subject(String book, String subject) {
        List<Question> list = bySubject.get(key(book, subject));
        return list == null ? Collections.emptyList() : list;
    }
    List<Question> collection(String book) {
        List<Question> list = books.get(book);
        return list == null ? Collections.emptyList() : list;
    }
    boolean validBook(String book) { return books.containsKey(book); }
    boolean validSubject(String subject) {
        for (String s : SUBJECTS) if (s.equals(subject)) return true;
        return false;
    }
    int indexOf(Question q) {
        List<Question> list = subject(q.book, q.subject);
        for (int i = 0; i < list.size(); i++) if (list.get(i).n == q.n) return i;
        return -1;
    }
    boolean validSavedId(String id) {
        for (String book : BOOK_IDS) {
            for (Question q : collection(book)) if (q.savedId().equals(id)) return true;
        }
        return false;
    }
    static String key(String book, String subject) { return book + "::" + subject; }
}
