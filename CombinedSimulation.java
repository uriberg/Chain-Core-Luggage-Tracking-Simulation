
import java.util.*;  // needed for Scanner
import java.lang.*;
import com.chain.api.*;
import com.chain.http.*;
import com.chain.signing.*;

//reader part
/*
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
*/
public class CombinedSimulation
{

    /* Menu,first option coming to a new flight,so register the suitcase. just hand in the passport and then will print to the screen it's flight,destination, and ask for the passenger to keep
     * if Gafni is flying with 2 suitcases, then just name them Gafni:1, Gafni:2
     * second option will be that Gafni lost his luggage...
     * When Gafni come to complain about his lost luggages, just ask him to hand in his passport, then print to the screen the luggages location and it's id, even if one of them has come back to Gafni
     * when he hand in his passport, look over his name on the owners list, then print the relevant info
     * 
     */
    // instance variables - replace the example below with your own
    private static double successProbability=0.4;
    private static double wrongFlight=0.85;
    private static double actualWrongFlight=0.9;
    private static double actualWrongAirport=0.6;
    private static boolean wrongTransfer=false;
    private static Scanner scanner = new Scanner( System.in );

    //reader  part
    /*
    public static final String IP_ADDRESS = new String("127.0.0.1");
    public static final int READER_PORT = 54322;
    private static Socket connection = null;

    private static PrintWriter out = null; //not in use
    private static BufferedReader in = null; //not in use
*/
    public static void main(String[] args) throws Exception { 
        //reader setup
/*
        init(); //init connection
        System.out.println("connection with server established");
        // Thread.sleep(2);
  */      
        Client client = new Client();      
        MockHsm.Key key = MockHsm.Key.create(client);
        HsmSigner.addKey(key, MockHsm.getSignerClient(client));
        
        //setup
        Account.Items setup = new Account.QueryBuilder()
            .setFilter("alias=$1")
            .addFilterParameter("Ben Gurion Airport")
            .execute(client);
        //put in the array of losts luggage, but with pair: <name,asset id> name==Owner   
        if (!setup.hasNext()){  
            new Account.Builder()
            .setAlias("Ben Gurion Airport")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);

            new Account.Builder()
            .setAlias("Budapest Airport")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);

            new Account.Builder()
            .setAlias("Istanbul Airport")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);

            new Account.Builder()
            .setAlias("Rome Airport")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);

            new Account.Builder()
            .setAlias("WZ9872")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);

            new Account.Builder()
            .setAlias("ISA282")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);

            new Account.Builder()
            .setAlias("LY351")
            .addRootXpub(key.xpub)
            .setQuorum(1)
            .create(client);
            //end of setup
        }
        while(true){
            System.out.println("1) I would like to check-in");
            System.out.println("2) I would like to locate my luggage");
        
            //on real scneario: read tag barcode of the asset and inform the passenger about the luggage location.
            //in our simulation, print debug to after register to flight to see if my luggage is lost or no
            //then,inform the passenger about it's location
            String chosen=scanner.next();
            //now check if the pressed option is either 1 or 2
            if (chosen.equals("1")){        //public static void setup(Client client,MockHsm.Key key) throws Exception 
                RegisterSuitcase(client,key);
            }

            else if (chosen.equals("2")){
                LocateMyLuggage(client,key);    
            }
            else{
                System.out.println("No valid option has been chosen,quitting simulation");
                break;
            }
        }

    }

    public static void RegisterSuitcase(Client client,MockHsm.Key key) throws Exception {
        //in reality,just hand in your passport for details
       
        String destination="";
        System.out.println("Please type your name");
        String name=scanner.next();
        System.out.println("Please type your id");
        String id=scanner.next();
        System.out.println("Please choose your destination");
        System.out.println("1) Rome");
        System.out.println("2) Istanbul");
        System.out.println("3) Budapest");
        String chosen=scanner.next();
        if (chosen.equals("1"))
            destination="Rome";
        else if (chosen.equals("2"))
            destination="Istanbul";
        else if (chosen.equals("3"))
            destination="Budapest";
        else
            System.out.println("No valid destination chosen");    
        //end of deatils
       
        //creating the account(pair of <name,id>)
        new Account.Builder()
        .setAlias(name)
        .addRootXpub(key.xpub)
        .setQuorum(1)
        .create(client);
        //adding the asset=suitcase
        new Asset.Builder()
        .setAlias(name+":"+id)
        .addRootXpub(key.xpub)
        .setQuorum(1)
            //.addTag("Location","Ben Gurion Airport")
        .addDefinitionField("Owner", name)
        .addDefinitionField("Id",id)
        .create(client);

        Transaction.Template Register = new Transaction.Builder()
            .addAction(new Transaction.Action.Issue()
                .setAssetAlias(name+":"+id)
                .setAmount(2)
            ).addAction(new Transaction.Action.ControlWithAccount()
                .setAccountAlias(name)
                .setAssetAlias(name+":"+id)
                .setAmount(2)
            ).build(client);

        Transaction.Template signed = HsmSigner.sign(Register);
        Transaction.submit(client,signed);

        //move suitcase to ben gurion airport
        Transaction.Template CheckIn = new Transaction.Builder()
            .addAction(new Transaction.Action.SpendFromAccount()
                .setAccountAlias(name)
                .setAssetAlias(name+":"+id)
                .setAmount(1)
            ).addAction(new Transaction.Action.ControlWithAccount()
                .setAccountAlias("Ben Gurion Airport")
                .setAssetAlias(name+":"+id)
                .setAmount(1)
            ).build(client);
        
        Transaction.Template signedCheckIn = HsmSigner.sign(CheckIn);
        Transaction.submit(client, signedCheckIn);
        // end of moving suitcase to ben gurion airport

        Simulate(client,key,name,id,destination);
    }    

    public static void Simulate(Client client,MockHsm.Key key, String Owner, String id, String destination) throws Exception {
        String Alias=Owner+":"+id;
        //System.out.println("this is the Alias:"+Alias);  
        UnspentOutput.Items acmeCommonUnspentOutputs = new UnspentOutput.QueryBuilder()
            .setFilter("(account_alias=$1) AND (asset_alias=$2)")
            .addFilterParameter("Ben Gurion Airport")
            .addFilterParameter(Alias)
            .execute(client);

        UnspentOutput utxo = acmeCommonUnspentOutputs.next();
       // System.out.println("debug: The current asset is starting simulation: "+ utxo.assetAlias);   
        Map newmap = utxo.assetDefinition;
        //String Owner=(String)newmap.get("Owner");
        //String airport="";
        String flight="";
        String wrongFlight1="";
        String wrongFlight2="";
        if (destination.equals("Rome")){
            //  airport="Rome Airport";
            flight="LY351";
            wrongFlight1="ISA282";
            wrongFlight2="WZ9872";
        }
        else if (destination.equals("Istanbul")){
            //  airport="Rome Airport";
            flight="ISA282";
            wrongFlight1="LY351";
            wrongFlight2="WZ9872";
        }
        else {
            flight="WZ9872";   
            wrongFlight1="LY351";
            wrongFlight2="ISA282";
        }

        double x = Math.random();
        //System.out.println("x value is: "+x);
        double y = Math.random();
        if (x<successProbability){ //move to proper flight
            wrongTransfer=false;
            // snippet build-retire
            Transaction.Template flightTransaction = new Transaction.Builder()
                .addAction(new Transaction.Action.SpendFromAccount()
                    .setAccountAlias("Ben Gurion Airport")
                    .setAssetAlias(utxo.assetAlias)
                    .setAmount(1)
                ).addAction(new Transaction.Action.ControlWithAccount()
                    .setAccountAlias(flight)
                    .setAssetAlias(utxo.assetAlias)
                    .setAmount(1)
                ).build(client);
            // endsnippet

            // snippet sign-retire
            Transaction.Template signedFlight = HsmSigner.sign(flightTransaction);
            // endsnippet

            // snippet submit-retire
            Transaction.submit(client, signedFlight);
            // endsnippet
            System.out.println("debug: luggage Moved to flight" + flight + "successfully! : " + utxo.assetAlias);

            if (y<successProbability){
                // snippet build-retire
                Transaction.Template completeCycle = new Transaction.Builder()
                    .addAction(new Transaction.Action.SpendFromAccount()
                        .setAccountAlias(flight)
                        .setAssetAlias(utxo.assetAlias)
                        .setAmount(1)
                    ).addAction(new Transaction.Action.ControlWithAccount()
                        .setAccountAlias(Owner)
                        .setAssetAlias(utxo.assetAlias)
                        .setAmount(1)
                    ).build(client);
                // endsnippet

                // snippet sign-retire
                Transaction.Template signedCompleteCycle = HsmSigner.sign(completeCycle);
                // endsnippet

                // snippet submit-retire
                Transaction.submit(client, signedCompleteCycle);
                // endsnippet
                System.out.println("debug: luggage Moved back to "+Owner+" from flight" + flight + "successfully! : " + utxo.assetAlias);

            }

        }
        else if (x<wrongFlight){ //luggage moved to the wrong plane

            String chosenWrongFlight="";   
            double draw=Math.random();
            if (draw <0.5){
                chosenWrongFlight=wrongFlight1;
            }
            else{
                chosenWrongFlight=wrongFlight2;
            }
            x=Math.random();
            y=Math.random();
            if (x<actualWrongFlight){ //move to the wrong plane
                wrongTransfer=true; //to signal a mistake when trying to locate the luggage

               //ben gurion to wrong flight
                Transaction.Template flightTransaction = new Transaction.Builder()
                    .addAction(new Transaction.Action.SpendFromAccount()
                        .setAccountAlias("Ben Gurion Airport")
                        .setAssetAlias(utxo.assetAlias)
                        .setAmount(1)
                    ).addAction(new Transaction.Action.ControlWithAccount()
                        .setAccountAlias(chosenWrongFlight)
                        .setAssetAlias(utxo.assetAlias)
                        .setAmount(1)
                    ).build(client);
               
                Transaction.Template signedFlight = HsmSigner.sign(flightTransaction);
                Transaction.submit(client, signedFlight);
                 // end of ben gurion to wrong flight
                
                if (y<actualWrongAirport){ //move to the wrong Airport
                    String wrongAirport="";
                    if (chosenWrongFlight.equals("LY351")){
                        wrongAirport="Rome Airport";
                    }
                    else if (chosenWrongFlight.equals("ISA282")){
                        wrongAirport="Istanbul Airport";
                    }
                    else if (chosenWrongFlight.equals("WZ9872")){
                        wrongAirport="Budapest Airport";
                    }
                    // snippet build-retire
                    Transaction.Template wrongAirportTransaction = new Transaction.Builder()
                        .addAction(new Transaction.Action.SpendFromAccount()
                            .setAccountAlias(chosenWrongFlight)
                            .setAssetAlias(utxo.assetAlias)
                            .setAmount(1)
                        ).addAction(new Transaction.Action.ControlWithAccount()
                            .setAccountAlias(wrongAirport)
                            .setAssetAlias(utxo.assetAlias)
                            .setAmount(1)
                        ).build(client);
                    
                    Transaction.Template signedWrongAirport = HsmSigner.sign(wrongAirportTransaction);
                    Transaction.submit(client, signedWrongAirport);
                    // end of moving to wrong airport
                    
                }   
            }
        }
    }

    public static void LocateMyLuggage(Client client,MockHsm.Key key) throws Exception {
        //in reality,just hand in your passport for details
        Scanner scanner = new Scanner( System.in );
        System.out.println("Please type your name");
        String name=scanner.next();
        System.out.println("Please type your id");
        String id=scanner.next();
        System.out.println("Please type your destination");
        String destination=scanner.next();
        //end of giving details
        boolean keepLooking=true;

        //checking if it wasn't delivered outside of ben gurion airpot at all
        UnspentOutput.Items FailedToMoveToFlight = new UnspentOutput.QueryBuilder()
            .setFilter("account_alias=$1")
            .addFilterParameter("Ben Gurion Airport")
            .execute(client);
          
        while (FailedToMoveToFlight.hasNext()){
            UnspentOutput utxo = FailedToMoveToFlight.next();
            if (utxo.assetAlias.equals(name+":"+id)){
                keepLooking=false;
                System.out.println("Dear " + name + ", your luggage is still in Ben gurion airport");
                System.out.println("Asset alias is: " + utxo.assetAlias);
            }
        }
        
        if (keepLooking){ //luggage isn't at ben gurion airport, check if it's on the plane
            //in reality, flight number and destination appears on the screen
            String flight="";
            if (destination.equals("Budapest"))
                flight="WZ9872";
            else if (destination.equals("Rome"))
                flight="LY351";
            else
                flight="ISA282";

            UnspentOutput.Items onThePlane = new UnspentOutput.QueryBuilder()
                .setFilter("account_alias=$1")
                .addFilterParameter(flight)
                .execute(client);
                    
            while (onThePlane.hasNext()) {
                UnspentOutput utxo = onThePlane.next();
                if (utxo.assetAlias.equals(name+":"+id)){
                    keepLooking=false;
                    System.out.println("Dear " + name + ", your luggage is still on the plane,flight "+flight);
                }
            }
        }

        if (keepLooking){//mistake in delivery,perhaps on another flight or airport
            
            String currentLuggageSituation=getTags(client,name,id);
            System.out.println(currentLuggageSituation);
            String wrongDestination="";
            if ((currentLuggageSituation.contains("ISA282")) || (currentLuggageSituation.contains("Istanbul")))
                wrongDestination="Istanbul Airport";
            else if ((currentLuggageSituation.contains("LY351")) || (currentLuggageSituation.contains("Rome")))
                wrongDestination="Rome Airpot";
            else  if ((currentLuggageSituation.contains("WZ9872")) || (currentLuggageSituation.contains("Budapest")))
                wrongDestination="Budapest Airport";    

            System.out.println("Dear " + name + ", there was a mistake, your luggage was delivered to "+wrongDestination);
        }
        /*
        UnspentOutput.Items inWZ9872 = new UnspentOutput.QueryBuilder()
        .setFilter("account_alias=$1")
        .addFilterParameter("WZ9872")
        .execute(client);
        //put in the array of losts luggage, but with pair: <name,asset id> name==Owner        
        while (inWZ9872.hasNext()) {
        UnspentOutput utxo = inWZ9872.next();
        if (utxo.assetAlias.equals(name+":"+id)){
        if (wrongTransfer){
        System.out.println("Dear " + name + ", there was a mistake. your luggage delivered to Budapest,current location is on flight WZ9872");
        }
        else{
        System.out.println("Dear " + name + ", your luggage is still on the plane,flight WZ9872");
        System.out.println("Asset alias is: " + utxo.assetAlias);
        }
        }
        }

        UnspentOutput.Items inISA282 = new UnspentOutput.QueryBuilder()
        .setFilter("account_alias=$1")
        .addFilterParameter("ISA282")
        .execute(client);
        //put in the array of losts luggage, but with pair: <name,asset id> name==Owner     
        while (inISA282.hasNext()) {
        UnspentOutput utxo = inISA282.next();
        if (utxo.assetAlias.equals(name+":"+id)){
        if (wrongTransfer){
        System.out.println("Dear " + name + ", there was a mistake. your luggage delivered to Istanbul,current location is on flight WZ9872");
        }
        else{
        System.out.println("Dear " + name + ", your luggage is still on the plane, flight ISA282");
        System.out.println("Asset alias is: " + utxo.assetAlias);
        }
        }
        }

        UnspentOutput.Items WrongAirport1 = new UnspentOutput.QueryBuilder()
        .setFilter("account_alias=$1")
        .addFilterParameter("Budapest Airport")
        .execute(client);
        //put in the array of losts luggage, but with pair: <name,asset id> name==Owner   
        while (WrongAirport1.hasNext()){
        UnspentOutput utxo = WrongAirport1.next();
        if (utxo.assetAlias.equals(name+":"+id)){
        System.out.println("Dear " + name + ", there was a mistake, your luggage delivered to Budapest Airport");
        System.out.println("Asset alias is: " + utxo.assetAlias);
        }
        }

        UnspentOutput.Items WrongAirport2 = new UnspentOutput.QueryBuilder()
        .setFilter("account_alias=$1")
        .addFilterParameter("Istanbul Airport")
        .execute(client);
        //put in the array of losts luggage, but with pair: <name,asset id> name==Owner   
        while (WrongAirport2.hasNext()){
        UnspentOutput utxo = WrongAirport2.next();
        if (utxo.assetAlias.equals(name+":"+id)){
        System.out.println("Dear " + name + ", there was a mistake, your luggage delivered to Istanbul Airport");
        System.out.println("Asset alias is: " + utxo.assetAlias);
        }
        }

        UnspentOutput.Items WrongAirport3 = new UnspentOutput.QueryBuilder()
        .setFilter("account_alias=$1")
        .addFilterParameter("Rome Airport")
        .execute(client);
        //put in the array of losts luggage, but with pair: <name,asset id> name==Owner   
        while (WrongAirport3.hasNext()){
        UnspentOutput utxo = WrongAirport3.next();
        if (utxo.assetAlias.equals(name+":"+id)){
        System.out.println("Dear " + name + ", there was a mistake, your luggage delivered to Rome Airport");
        System.out.println("Asset alias is: " + utxo.assetAlias);
        }
        }
         */
    }

    /**
     * Initialize the connection and send username/password

     //* @throws IOException
     //* @throws InterruptedException
     */
    /*
    private static void  init() throws IOException, InterruptedException{
        connection = new Socket("127.0.0.1", READER_PORT);
        System.out.println("made the connection");
        in = new BufferedReader(new InputStreamReader(connection
                .getInputStream()));
        System.out.println("made after in");        
        out = new PrintWriter(connection.getOutputStream());
        System.out.println("made after out");
        //Thread.sleep(2);
        System.out.println("after sleep");
        //System.out.println(readFromReader(in));
        System.out.println("made after read from reader");
        out.write("alien\n");
        out.flush();
        System.out.println("made after flush");
        // System.out.println(readFromReader(in));
        //Thread.sleep(500);
        out.write("password\n");
        out.flush();
        // System.out.println(readFromReader(in));
    }
*/
    /**
     * Get tags back from the alien reader
     * @return
     * @throws IOException
     */
    private static String getTags(Client client, String name, String id) throws Exception{
        String Alias=name+":"+id; 
        String Owner="";
        String currentLocation="";
        UnspentOutput.Items possibleLocation = new UnspentOutput.QueryBuilder()
            .setFilter("asset_alias=$1")
            .addFilterParameter(Alias)
            .execute(client);
        
        while (possibleLocation.hasNext()){
            UnspentOutput utxo = possibleLocation.next();
         
            if (!utxo.accountAlias.equals(name)){
                currentLocation=utxo.accountAlias;
                Map newmap = utxo.assetDefinition;
                Owner=(String)newmap.get("Owner");
            }
        }   
       
        String currentSituation="Owner is: "+Owner+" , current location is: "+currentLocation; 
        //   String returnVal = readFromReader(in);
        return currentSituation;
    }

    
    
    /**
     * Read responses from the socket
  //   * @param inBuf
    // * @return
    // * @throws IOException
     */
    /*
    public static String readFromReader(BufferedReader inBuf) throws IOException{
        StringBuffer buf=new StringBuffer();
        System.out.println("in read from reader");
        int ch=inBuf.read();
        while((char)ch!='\0'){
            // System.out.println("read from reader,in the loop");
            buf.append((char)ch);
            ch=inBuf.read();
        }
        System.out.println("after while loop");
        return buf.toString();
    }
    */

}
