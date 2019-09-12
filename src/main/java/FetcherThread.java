import javax.mail.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class FetcherThread implements Runnable{

    private List<Message> allMsgs;
    private AtomicInteger index;
    private String usr;
    private String pwd;
    private int chunkSize;
    public FetcherThread(List<Message> am, AtomicInteger i, String u, String p, int cs){
        allMsgs = am;
        index = i;
        usr = u;
        pwd = p;
        chunkSize = cs;
    }

    @Override
    public void run() {
        try {
            //Connect to gmail with given cardentials
            Session session = Session.getDefaultInstance(new Properties( ));
            Store store = session.getStore("imaps");
            store.connect("imap.googlemail.com", 993, usr, pwd);
            Folder inbox = store.getFolder( "INBOX" );
            inbox.open( Folder.READ_ONLY );

            //Get the total number of emails in the inbox
            int totalMsgs = inbox.getMessageCount();

            //Get the emails in chunks and add them to allMsgs
            for(;;) {
                int i = index.getAndIncrement();
                //The last chunk (May not be of desired chunksize) break.
                if ((totalMsgs/chunkSize) == i && (totalMsgs%chunkSize != 0)){
                    Message [] chunk = inbox.getMessages((i*chunkSize)+1,totalMsgs);
                    for(Message m : chunk) allMsgs.add(m);
                    break;
                //All messages have been collected, break.
                }else if ((i * chunkSize) >= totalMsgs){
                    break;
                //More messages to be added, add this chunk and loop
                }else{
                    Message [] chunk = inbox.getMessages((i*chunkSize)+1,(i*chunkSize)+chunkSize);
                    for(Message m : chunk) allMsgs.add(m);
                }
            }
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            return;
        } catch (MessagingException e) {
            e.printStackTrace();
            return;
        }
    }
}
