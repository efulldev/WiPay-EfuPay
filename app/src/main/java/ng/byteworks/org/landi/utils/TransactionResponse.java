package ng.byteworks.org.landi.utils;

import com.arke.sdk.util.epms.Transaction;

public class TransactionResponse {
    private String details;
    private Transaction transaction;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}
