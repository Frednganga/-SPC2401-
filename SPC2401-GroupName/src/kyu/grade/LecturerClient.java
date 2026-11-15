package kyu.grade;

/** Usage: LecturerClient submit <studentId> <unit> <mark> | publish <unit> */
public class LecturerClient {
    public static void main(String[] a) {
        Config c = Config.load();
        GradeClient g = new GradeClient(c);
        try {
            if (a.length == 4 && a[0].equals("submit")) {
                g.submitMark(a[1], a[2], Integer.parseInt(a[3]));
                System.out.println("OK: submitted " + a[1] + "/" + a[2] + "=" + a[3]);
            } else if (a.length == 2 && a[0].equals("publish")) {
                g.publishMarks(a[1]);
                System.out.println("OK: published " + a[1]);
            } else {
                System.out.println("Usage: LecturerClient submit <studentId> <unit> <mark> | publish <unit>");
            }
        } catch (GradeException e) {
            System.out.println("REJECTED: " + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            Log.log("ERROR", "Lecturer call failed: " + e);
            System.exit(1);
        }
    }
}
