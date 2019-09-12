import javax.mail.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Main {
    public static void main(String [] args){

        //uncomment for manual credentials input
        //Scanner kb = new Scanner(System.in);
        //System.out.println("Username?");
        //String usr = kb.nextLine();
        //System.out.println("Password?");
        //String pwd = kb.nextLine();

        String usr = "csc344testacc@gmail.com";
        String pwd = "T3st123A";

        Message [] msgsSeq = fetchInboxSeq(usr,pwd);

        List<Message> msgsCon = fetchInboxCon(usr,pwd, msgsSeq.length,5,10);

        List<Message> msgsConB = fetchInboxConB(usr,pwd, msgsSeq.length,5,10);


        System.out.println("Sequential-------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        printMessages(msgsSeq);
        System.out.println("concurrent-A-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        printMessages(msgsCon.toArray(new Message [msgsSeq.length]));
        System.out.println("concurrent-B-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        printMessages(msgsConB.toArray(new Message [msgsSeq.length]));

    }


    //Fetches the messages concurrently, where each thread spawns it's own javamail session
    private static List<Message> fetchInboxCon(String usr, String pwd, int numEmails, int numthreads, int chunksize) {
        List<Message> allmsgs = Collections.synchronizedList(new ArrayList<>());
        try{

            AtomicInteger index = new AtomicInteger(0);
            System.out.print("Fetching messages Concurrently using method A...");
            float start = System.nanoTime();

            List<Thread> fetchers = Stream
                    .generate(() -> new Thread(new FetcherThread(allmsgs, index, usr, pwd, chunksize)))
                    .limit(numthreads)      // sets the number of threads to spawn
                    .collect(toList());     // Collects them into a list
            fetchers.forEach(Thread::start);// Starts the threads

            //This line forces the main thread to wait untill allmsgs is full
            while(allmsgs.size() < numEmails){}

            System.out.println("Done. ( " + (System.nanoTime() - start) + " ns )");
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return allmsgs;
    }

    //Fetches the messages concurrently, where each thread uses the same javamail session
    //(Slower, javamail doesn't seem friendly to concurrent use)
    private static List<Message> fetchInboxConB(String usr, String pwd, int numEmails, int numthreads, int chunksize) {
        List<Message> allmsgs = Collections.synchronizedList(new ArrayList<>());
        try{
            Session session = Session.getDefaultInstance(new Properties( ));
            Store store = session.getStore("imaps");
            store.connect("imap.googlemail.com", 993, usr, pwd);
            Folder inbox = store.getFolder( "INBOX" );
            inbox.open( Folder.READ_ONLY );

            AtomicInteger index = new AtomicInteger(0);
            System.out.print("Fetching messages Concurrently using method B...");
            float start = System.nanoTime();

            List<Thread> fetchers = Stream
                    .generate(() -> new Thread(new FetcherThreadTypeB(allmsgs, index, inbox, chunksize)))
                    .limit(numthreads)      // Limits the number of threads to spawn
                    .collect(toList());     // Collects them into a list
            fetchers.forEach(Thread::start);// Starts the threads

            //This line forces the main thread to wait untill allmsgs is full
            while(allmsgs.size() < numEmails){}

            System.out.println("Done. ( " + (System.nanoTime() - start) + " ns )");
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
        return allmsgs;
    }

    //Fetches messages in a sequential fashion.
    public static Message [] fetchInboxSeq(String usr, String pwd){
        Message [] messages = null;
        try{
            Session session = Session.getDefaultInstance(new Properties( ));
            Store store = session.getStore("imaps");
            store.connect("imap.googlemail.com", 993, usr, pwd);

            System.out.print("Fetching messages Sequentially...");
            float start = System.nanoTime();
            Folder inbox = store.getFolder( "INBOX" );
            inbox.open( Folder.READ_ONLY );
            messages =  inbox.getMessages(1,inbox.getMessageCount());
            System.out.println("Done. ( " + (System.nanoTime() - start) + " ns )");

        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }

        return messages;
    }

    public static void printMessages(Message [] msgs){
        for(Message m : msgs){
            try {
                System.out.printf("%1$-150s "+ m.getReceivedDate().toString()+"\n", m.getSubject());
            } catch (MessagingException e) {
                e.printStackTrace();
            }

        }
    }
}
