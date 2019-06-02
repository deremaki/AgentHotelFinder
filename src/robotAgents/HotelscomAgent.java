package robotAgents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import utils.*;

public class HotelscomAgent extends Agent {

    private String destination;
    private Date dateFrom;
    private Date dateTo;
    private int minimumRating;

    private int totalWait = 120;
    private int wait = 0;


    @Override
    protected void setup() {
        super.setup();

        registerToDF();

        Object[] args = getArguments();
        if (args != null) {
            destination = (String) args[0];
            dateFrom = (Date) args[1];
            dateTo = (Date) args[2];
            minimumRating = (int) args[3];
        }

        addBehaviour(new myReceiver(this, 60000,  MessageTemplate.MatchPerformative(ACLMessage.REQUEST)) {
            public void handle( ACLMessage msg)
            {
                if (msg != null ) {
                    myAgent.addBehaviour(new OneShotBehaviour() {
                        @Override
                        public void action() {
                            try {
                                Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"\"C:\\Users\\derem\\AppData\\Local\\UiPath\\app-19.4.2\\UiRobot.exe\" -file \"C:\\Users\\derem\\OneDrive\\Dokumenty\\UiPath\\bookingcomAgent\\Main.xaml\"&& exit\"");
                                System.out.println("test");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            File output = new File("C:\\Users\\derem\\Desktop\\warszawa.csv");
                            wait = 0;
                            while(!output.exists() && wait<totalWait) {
                                try {
                                    Thread.sleep(1000);
                                    wait++;
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            System.out.println("robotAgents.HotelscomAgent: Found nice hotel for 500!");
                            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                            message.setContent("500");
                            message.addReceiver(new AID( "mainAgent", AID.ISLOCALNAME));
                            send(message);
                        }
                    });
                }
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
