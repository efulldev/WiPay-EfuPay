package ng.byteworks.org.landi.utils;

import com.arke.sdk.util.epms.Transaction;

public interface TransactionCompleteCallback {
    void done(Transaction transaction);
}
