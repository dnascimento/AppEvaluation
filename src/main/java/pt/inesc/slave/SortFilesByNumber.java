package pt.inesc.slave;

import java.io.File;
import java.util.Comparator;

public class SortFilesByNumber
        implements Comparator<File> {

    public int compare(File o1, File o2) {
        String name1 = o1.getName().replaceAll("\\D+", "");
        String name2 = o2.getName().replaceAll("\\D+", "");
        if (name1.length() == 0 || name2.length() == 0) {
            return o1.getName().compareTo(o2.getName());
        }
        int id1 = Integer.parseInt(name1);
        int id2 = Integer.parseInt(name2);
        return id1 - id2;
    }
}
