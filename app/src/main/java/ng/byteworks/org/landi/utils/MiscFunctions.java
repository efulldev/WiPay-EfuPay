package ng.byteworks.org.landi.utils;

import com.arke.sdk.util.data.BytesUtil;
import com.arke.sdk.util.epms.Transaction;

import java.util.StringTokenizer;

public class MiscFunctions {

    public static String CardScheme(Transaction transaction){
        return new String(BytesUtil.hexString2ByteArray(transaction.getLabel()));
    }

    public static String getMaskPAN(String track2) {
        String maskedPan = "";
        String pan = "";
        if (track2 != null && !track2.isEmpty()) {
            StringTokenizer st;
            if (track2.indexOf(68) > -1) {
                st = new StringTokenizer(track2, "D");
            } else {
                st = new StringTokenizer(track2, "=");
            }

            pan = st.nextToken();
        }

        if (pan != null && !pan.isEmpty()) {
            int no = pan.length() - 10;
            String stars = "";

            for(int i = 0; i < no; ++i) {
                stars = stars + "X";
            }

            maskedPan = pan.substring(0, 6) + stars + pan.substring(pan.length() - 4);
        }

        return maskedPan;
    }

    public static String getCustomerName(Transaction transaction){
        return new String(BytesUtil.hexString2ByteArray(transaction.getCardholdername()));
    }
}
