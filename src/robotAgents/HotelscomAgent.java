package robotAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.Date;

import utils.*;

public class HotelscomAgent extends Agent {

    private String destination;
    private String dateFrom;
    private String dateTo;
    private int minimumRating;
    private int adults;

    private int totalWait = 120;
    private int wait = 0;


    @Override
    protected void setup() {
        super.setup();

        registerToDF();

        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                addBehaviour(new myReceiver(myAgent, 60000,  MessageTemplate.MatchPerformative(ACLMessage.REQUEST)) {
                    public void handle( ACLMessage msg)
                    {
                        if (msg != null ) {

                            //decode message
                            String content = msg.getContent();
                            String[] args = content.split(";");
                            destination = args[0];
                            dateFrom = args[1];
                            dateTo = args[2];
                            minimumRating = Integer.parseInt(args[3]);
                            adults = Integer.parseInt(args[4]);

                            String workDirectory = "C:\\Robot\\";

                            String parameters = "\"{'destination': '"+destination+"', 'dateFrom': '"+dateFrom+"', 'dateTo': '"+dateTo+"', 'minimumRating': "+minimumRating+", 'adults': "+adults+" }\"";

                            myAgent.addBehaviour(new OneShotBehaviour() {
                                @Override
                                public void action() {
                                    try {
                                        Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"\"C:\\Users\\derem\\AppData\\Local\\UiPath\\app-19.4.2\\UiRobot.exe\" -file \"C:\\Users\\derem\\source\\repos\\AgentHotelFinder\\UiPath\\bookingcomAgent\\Main.xaml\" -input " + parameters + "&& exit\"");
                                        //Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"\"C:\\Users\\Piotrek\\AppData\\Local\\UiPath\\app-19.5.0\\UiRobot.exe\" -file \"C:\\Users\\Piotrek\\Desktop\\AgentHotelFinder\\UiPath\\bookingcomAgent\\Main.xaml\" -input " + parameters + "&& exit\"");

                                        System.out.println("test");
                                    } catch (IOException e) {
                                        //e.printStackTrace();
                                    }

                                    File output = new File(workDirectory + destination +".csv");
                                    wait = 0;
                                    while(!output.exists() && wait<totalWait) {
                                        try {
                                            Thread.sleep(1000);
                                            wait++;
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    //read output
                                    var result = CSVHelper.ReadCsv(workDirectory + destination +".csv");
                                    //delete output
                                    output.delete();

                                    //notify MainAgent
                                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                                    message.setContent("500");
                                    message.addReceiver(new AID( "mainAgent", AID.ISLOCALNAME));
                                    try {
                                        HotelsResult r = new HotelsResult();
                                        r.result = result;
                                        message.setLanguage("English");
                                        message.setContentObject(r);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    send(message);
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void registerToDF() {
        ServiceDescription sd = new ServiceDescription();
        sd.setType("HotelRobot");
        sd.setName(getLocalName());

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

    }
}
