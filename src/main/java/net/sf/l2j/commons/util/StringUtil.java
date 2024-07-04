package net.sf.l2j.commons.util;

/**
 * @author finfan
 */
public class StringUtil {

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        str = str.replace("_", " ");

        StringBuilder sb = new StringBuilder();
        boolean nextUpperCase = true;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (Character.isWhitespace(c)) {
                sb.append(c);
                nextUpperCase = true;
            } else if (nextUpperCase) {
                sb.append(Character.toUpperCase(c));
                nextUpperCase = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }

        return sb.toString();
    }

}
