import javax.mail.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FetcherThreadTypeB implements Runnable {

    private List<Message> allMsgs;
    private AtomicInteger index;
    private Folder inbox;
    private int chunkSize;
    public FetcherThreadTypeB(List<Message> am, AtomicInteger i, Folder ib, int cs){
        allMsgs = am;
        index = i;
        inbox = ib;
        chunkSize = cs;
    }

    @Override
    public void run() {
        try {
            //Get the total number of emails in the inbox
            int totalMsgs = inbox.getMessageCount();

            //Get the emails in chunks and add them to allMsgs
            for(;;) {
                int i = index.getAndIncrement();
                //The last chunk (May not be of correct size) break.
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
