import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class Firebase {
    private IDriver driver;
    private DateFormat dateFormat;

    public Firebase() {
        this.driver = new Driver();
        this.dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String key = "";
        try {
            key = readFile(Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.driver.setKey(key);
    }

    private static String readFile(Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get("key.txt"));
        return new String(encoded, encoding);
    }

    private String getDate() {
        return dateFormat.format(new Date().getTime());
    }

    public boolean createStudent(String number, String club) {
        addStudentToList(number);
        this.driver.setChannel(number, club);
        Map<String, String> data = new HashMap<>();
        data.put("meeting1", getDate());
        return this.driver.writeV2(data);

    }

    private void addStudentToList(String number) {
        HashSet<String> all_students = new HashSet<>();
        this.driver.resetChannel();
        this.driver.setChannel("all_students");
        Reader read = this.driver.read();
        this.driver.resetChannel();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(read);
        if (element.toString().equalsIgnoreCase("null")) {
            this.driver.writeA("all_students", all_students.toArray(new String[0]));
        } else {
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                all_students.add(jsonElement.getAsString());
            }
            if (!number.equals("club_master")) {
                    HashMap<String, String> tt = new HashMap<>();
                    tt.put(String.valueOf(all_students.size()), number);
                    this.driver.resetChannel();
                    this.driver.setChannel("all_students");
                    this.driver.writeV2(tt);

            }
            else if (!all_students.contains("club_master")) {
                all_students.add(number);
                HashMap<String, String> tt = new HashMap<>();
                tt.put(String.valueOf(all_students.size()-1), number);
                this.driver.resetChannel();
                this.driver.setChannel("all_students");
                this.driver.writeV2(tt);
            }

//            this.driver.setChannel("all_students");
//            this.driver.writeA(all_students.toArray(new String[0]));
        }

    }


    private ArrayList<String> getAllStudents() {
        this.driver.resetChannel();
        this.driver.setChannel("all_students");
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(this.driver.read());
        if (!element.toString().equals("null")) {
            ArrayList<String> temp = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) {
                temp.add(e.getAsString());
            }
            return temp;
        } else {
            return null;
        }
    }

    private int SetMeetingDay(String number, String club) {
        Map<String, String> data = new HashMap<String, String>();
        this.driver.setChannel(number, club);
        // Reads data and parse as JSON
        Reader read = this.driver.read();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(read);
        if (element.toString().equals("null")) {
            boolean result = createStudent(number, club);
            if (result) {
                return 0;
            } else {
                return 1;
            }
        }
        Set<Map.Entry<String, JsonElement>> entries = element.getAsJsonObject().entrySet(); //will return members of your object
        String MaxKey;
        int MaxMeeting = 0;
        if (getPaid(number, club)) {
            MaxMeeting = (entries.size() - 1);
            MaxKey = "meeting" + MaxMeeting;
        } else {
            MaxMeeting = entries.size();
            MaxKey = "meeting" + MaxMeeting; // Sets current meeting day that is in the database
        }

        JsonElement MaxValue = null; // this will be assigned when the MaxKey is found
        for (Map.Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
//            data.put(key.toLowerCase(), value.toString().substring(1, value.toString().length() - 1).toLowerCase());

            if (key.equals(MaxKey)) {
                // Assigns max value which is date
                MaxValue = value;
            }
        }
        String MaxValueString;
        if (MaxValue == null) {
            return 1;
        }
        MaxValueString = MaxValue.getAsString().split(" ")[0];
        String date = getDate().split(" ")[0];
        // Checks if max value is today if not then signs user in
        if (!(MaxValueString.equals(date))) {
            // Sign user in for  today
            data.put("meeting" + (MaxMeeting + 1), getDate());
            this.driver.setChannel(number, club);
            boolean result = this.driver.writeV2(data);
            if (result) {
                System.out.println(number + " Signed IN");
                return 0;
            } else {
                return 1;
            }
        } else {
            System.out.println(number + " ALREADY IN");
            // TODO: CHANGE TO 2 - CURRENTLY BREAKS THREADING (Somehow threading calls this twice)
            return 0;
        }
    }

    int addMeetingDay(String number, String club) {
        ArrayList<String> allStudents = getAllStudents();
        if (allStudents == null) {
            boolean result = createStudent(number, club) == createStudent("club_master", club);
            if (result) {
                return 0;
            } else {
                return 1;
            }
        } else if (!allStudents.contains(number)) {
            boolean result = createStudent(number, club);
            int result2 = SetMeetingDay("club_master", club);
            if (result) {
                return result2;
            } else {
                return 1;
            }
        }
        int result = SetMeetingDay(number, club);
        int result2 = SetMeetingDay("club_master", club);
        if (result == 1 || result2 == 1) {
            return 1;
        }
        if (result == 2) {
            return 2;
        }
        return 0;

    }

    boolean getPaid(String number, String club) {
        this.driver.setChannel(number, club, "paid");
        Reader read = this.driver.read();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(read);
        return !element.toString().equalsIgnoreCase("null");
    }


    private boolean setUserPaid(String number, String club) {
        Map<String, String> data = new HashMap<>();
        data.put("paid", getDate());
        if (!getAllStudents().contains(number)) {
            return false;
        }
        this.driver.setChannel(number, club);
        return this.driver.writeV2(data);


//        Reader read = this.driver.read();
//        JsonParser parser = new JsonParser();
//        JsonElement element = parser.parse(read);
//        Set<Map.Entry<String, JsonElement>> entries = element.getAsJsonObject().entrySet(); //will return members of your object
//        for (Map.Entry<String, JsonElement> entry : entries) {
//            String key = entry.getKey();
//            JsonElement value = entry.getValue();
//            data.put(key.toLowerCase(), value.toString().substring(1, value.toString().length() - 1).toLowerCase());
//        }
//        this.driver.setChannel(number, club);
//        return this.driver.write(data);
    }

    boolean setPaid(String number, String club) {
        return setUserPaid(number, club) == setUserPaid("club_master", club);
    }

//    public static void main(String[] args) {
//        Firebase firebase = new Firebase();
//        firebase.addMeetingDay("641852934", "science");
//        firebase.setPaid("641852934", "science");
//
//
//        Random r = new Random();
//
//        String[] clubs = new String[]{"Art Club Ideas", "Sculpture Club", "Photography Club", "Art History Club", "Drama Club Ideas ", "Shakespeare Club", "Classics Club", "Monologue Club", "Comedy Sportz Club", "Improv Club", "Film Club Ideas", "Foreign Film Club", "Screenwriting Club", "Directing Club", "48-Hour Film Festival Club", "Science Club Ideas", "Future Scientists Club", "Marine Biology Club", "Future Medical Professionals Club", "Math Club Ideas", "Math Homework Club", "Pi Club", "Literature Club Ideas", "Literature Magazine Club", "Creative Writing Club", "Book Club", "Foreign Book Club", "History Club Ideas", "Ancient History Club", "Language Club Ideas", "Anime Club", "Chess Club", "Video Games Club", "Skiing Club", "Religion Club", "Adventure Club", "Charity Club Ideas", "Save Endangered Species Club"};
//        for (int i = 0; i < 100; i++) {
//            String data = String.valueOf(r.nextInt(900000000) + 100000000);
//            String club = clubs[r.nextInt(clubs.length)].replace(" ", "-");
//
//        }
//        firebase.addMeetingDay("741852936", "science");
//        firebase.addMeetingDay("741852936", "computer-science");
//        firebase.addMeetingDay("641852934", "science");
//        firebase.addMeetingDay("641852934", "computer-science");
//        firebase.addMeetingDay("641852934", "mathletes");
//
//
//    }
}


